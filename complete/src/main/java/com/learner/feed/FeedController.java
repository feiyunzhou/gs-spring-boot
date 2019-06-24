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
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

/**
 * 一些注意点：1. Cassandra 中的timestamp应该对应的java类是Date
 *            2. timeuuid对应的是UUID
 *
 * 设计要点： 1. tweet的数据sharding问题：
 *              1） 根据用户userId来sharding，这会导致热点用户的数据过于集中。
 *              2） 如果根据tweetId来sharding, 这会导致在查询数据的时候，数据过于分散，查询有延迟。
 *              3） 我们可以组合的方式来构建id，前半部分使用当前时间的秒数，后半部分用每个秒内自增的数字来表达
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

    @PostMapping("/friend")
    public ResponseEntity addFriend(@RequestBody Friend friend) {
        if (Strings.isNullOrEmpty(friend.getUserName()) || Strings.isNullOrEmpty(friend.getFriend())) {
            return ResponseEntity.badRequest().body("username or friend is empty or null");
        }
        if (friend.getUserName().equalsIgnoreCase(friend.getFriend())) {
            return ResponseEntity.badRequest().body("self can not to be friend");
        }

        friend.setSince(new Date());
        friendRepository.save(friend);
        Follower follower = Follower.createObject(friend.getFriend(), friend.getUserName());
        followerRepository.save(follower);

        return ResponseEntity.ok().build();
    }
    @PostMapping("/tweet")
    public ResponseEntity tweet(HttpServletRequest request, @RequestBody Tweet tweet) {
        if (Strings.isNullOrEmpty(tweet.getUserName()) || Strings.isNullOrEmpty(tweet.getBody())) {
            return ResponseEntity.badRequest().body("user name or body is empty");
        }

        if (!userRepository.existsByUserName(tweet.getUserName())) {
            return ResponseEntity.badRequest().body("user not exist");
        }
        //create tweet
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
        for (Follower follower : followers) {
            timelineRecords.add(TimelineRecord.createTimelineRec(follower.getFollower(), tweet.getTweetId()));
        }
        timelineRepository.saveAll(timelineRecords);
        /// 创建用户timeline完成

        return ResponseEntity.ok().build();
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
