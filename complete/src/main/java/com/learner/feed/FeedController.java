package com.learner.feed;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.learner.*;
import com.learner.messager.InboxMessage;
import com.learner.messager.InboxMessageRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * 一些注意点：1. Cassandra 中的timestamp应该对应的java类是Date
 *            2. timeuuid对应的是UUID
 *            3. spring data, 返回的page是有状态的，需要多次请求才能定位到对应的page
 *
 * 设计要点： 1. tweet的数据sharding问题：
 *              1） 根据用户userId来sharding，这会导致热点用户的数据过于集中。
 *              2） 如果根据tweetId来sharding, 这会导致在查询数据的时候，数据过于分散，查询有延迟。
 *              3） 我们可以组合的方式来构建id，前半部分使用当前时间的秒数，后半部分用每个秒内自增的数字来表达
 *           2. 设计了下面这些模型
 *              1）timeline 用于保存用户的timeline，主键是username+time, time是排序的。当用户发tweet的时候，将tweet发送给对应粉丝的inbox里
 *              2）Follower 用户的follower 列表
 *              3）Friend 用户的好友信息，也就是用户追随的用户列表
 *              4）Tweet tweet列表， tweetId是主键，使用time based UUID作为ID值
 *
 */

@RestController
@Log4j2
@RequestMapping("/feed")
public class FeedController {
    @Autowired
    TweetRepository tweetRepository;
    @Autowired
    FollowerRepository followerRepository;

    @Autowired
    TimelineRepository timelineRepository;

    @Autowired
    FriendRepository friendRepository;

    @Autowired
    UserRepository userRepository;

    /**
     * 用户增加新的朋友
     * @param friend
     * @return
     */
    @PostMapping("/friend")
    public ResponseEntity addFriend(@RequestBody Friend friend) {
        if (Strings.isNullOrEmpty(friend.getUserName()) || Strings.isNullOrEmpty(friend.getFriend())) {
            return ResponseEntity.badRequest().body("username or friend is empty or null");
        }
        if (friend.getUserName().equalsIgnoreCase(friend.getFriend())) {
            return ResponseEntity.badRequest().body("self can not to be friend");
        }
        if (!userRepository.existsByUserName(friend.getUserName()) ||  !userRepository.existsByUserName(friend.getFriend())) {
            return ResponseEntity.badRequest().body("the user not existing!");
        }

        friend.setSince(new Date());
        friendRepository.save(friend);
        Follower follower = Follower.createObject(friend.getFriend(), friend.getUserName());
        followerRepository.save(follower);

        return ResponseEntity.ok().build();
    }

    /**
     * 发tweet接口
     * @param request
     * @param tweet
     * @return
     */
    @PostMapping("/tweet")
    public ResponseEntity tweet(HttpServletRequest request, @RequestBody Tweet tweet) {
        if (Strings.isNullOrEmpty(tweet.getUserName()) || Strings.isNullOrEmpty(tweet.getBody())) {
            return ResponseEntity.badRequest().body("user name or body is empty");
        }

        if (!userRepository.existsByUserName(tweet.getUserName())) {
            return ResponseEntity.badRequest().body("user not exist");
        }
        //create tweet
        //tweet ID 可以使用当前时间的timestamp（精确到秒）+ 每秒reset为0的自增数
        tweet.setTweetId(UUIDs.timeBased());
        tweetRepository.save(tweet);

        //query the followers Note: 如果这个用户的follower很多，这个内存使用量很高，保存的时候也很多
        //使用spark来完成这种操作？这里应该创建任务，并且把任务放到队列中去处理
        /**
         *  timeline，根据username来分区，在一个分区内使用创建时间来排序
         *  直接用spark sql就能搞定，但是实时性不行
         */
        List<Follower> followers = followerRepository.findFollowersByUserName(tweet.getUserName());

        List<TimelineRecord> timelineRecords = Lists.newArrayList();
        // 自己也需要发一份
        timelineRecords.add(TimelineRecord.createTimelineRec(tweet.getUserName(), tweet.getTweetId()));
        for (Follower follower : followers) {
            timelineRecords.add(TimelineRecord.createTimelineRec(follower.getFollower(), tweet.getTweetId()));
        }
        timelineRepository.saveAll(timelineRecords);
        /// 创建用户timeline完成

        return ResponseEntity.ok().build();
    }

    /**
     * 获取用户的timeline信息
     * @param userName
     * @param uuid
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/timeline")
    public ResponseEntity fetchTimeline(String userName, String uuid, Integer page, Integer pageSize) {
        if (pageSize == null || pageSize == 0) {
            pageSize = 20;
        }

        if (page == null) {
            page = 0;
        }
        if (!userRepository.existsByUserName(userName)) {
            return ResponseEntity.badRequest().body("the user not existing!");
        }
        UUID lastUUID = UUIDs.timeBased();
        if (Strings.isNullOrEmpty(uuid)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date date = sdf.parse("2010-10-10");
                lastUUID = UUIDs.startOf(date.getTime());
                log.info(lastUUID.toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        //Pageable pageable = new Pageable();
        PageRequest pageable = PageRequest.of(0, pageSize);
        Slice<TimelineRecord> records = timelineRepository.findTimelineRecordsByUserNameAndTimeGreaterThan(userName, lastUUID, pageable);

        int i = 0;
        while (i < page && records.hasNext()) {
            Pageable next = records.nextPageable();
            records = timelineRepository.findTimelineRecordsByUserNameAndTimeGreaterThan(userName, lastUUID, next);
            i++;
        }
        List<UUID> tIds = records.get().map(rec -> {
            return rec.getTweetId();
        }).collect(Collectors.toList());
        List<Tweet> tweets = tweetRepository.findAllByTweetIdIn(tIds);

        for (Tweet tweet : tweets) {
            tweet.setDate(Utils.timeUUID2Date(tweet.getTweetId()));
        }
        return ResponseEntity.ok().body(tweets);
    }

    @PostMapping("/user")
    public ResponseEntity tweet(HttpServletRequest request, @RequestBody User user) {
        if (Strings.isNullOrEmpty(user.getUserName()) || Strings.isNullOrEmpty(user.getPassword())) {
            return ResponseEntity.badRequest().body("username/password is empty");
        }
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
