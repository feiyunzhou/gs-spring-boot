package com.learner.lbs;

import ch.hsr.geohash.GeoHash;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.learner.common.MessageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 *   1. 司机位置信息上传到Location 表 username_createtime -> [lat,lng,geohash]，司机每隔3秒更新位置
 *      1M司机/3s = 300k/s QPS
 *
 *   2. 根据位置更新信息为geohash建立索引，只取geohash的前5位作为索引，可以根据geohash查到对应的司机的locationId，也就是geohash->[locationId1, locationId2]
 *   3. 使用Redis， Redis的QPS能到 > 100k QPS
 *   4. 叫车过程： rider（叫车人） driver（司机）
 *       1）rider发起叫车请求，也就是发送一个 ride request。
 *       2）服务器收到这个请求后，调用matching服务器获取最近的几个driver，通常还需要调用ETA服务器估算driver到达时间，根据最少ETA排序。
 *       3）发送消息给top3的司机。
 *       4）司机收到消息后，确认是否需要接单。如果接单，需要设置trip中的driver的名字和状态。
 *       5）rider不断pull单子的状态, 一旦查询到有司机接单，则显示司机的信息。
 *       6）司机到达目的地后，行程开始
 *
 *
 *
 *
 */

@RestController
@Log4j2
@RequestMapping("/lbs")
public class LocationController {
    private final static int PRECISION_OF_NUM_OF_CHARACTERS = 7;

    private final static int LENGTH_OF_PRFIX = 5;
    private static final String LAT_KEY = "lat";
    private static final String LNG_KEY = "lng";
    private static final String GEO_HASH_KEY = "geohash";
    private static final String CREATE_TIME = "create_time";

    private static final String PUSH_MESSAGE_USERNAME = "LBS_ADMIN";
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private InterestingPointRepository interestingPointRepository;
    @Autowired
    private MessageService messageService;

    private Map<String, RateLimiter>  rateLimiters = Maps.newConcurrentMap();

    @PostMapping("/driver/location")
    public ResponseEntity driverLocationReport(@RequestBody InterestingPoint interestingPoint, String userName) {
        rateLimiters.putIfAbsent(userName, RateLimiter.create(0.1));

        if (!rateLimiters.get(userName).tryAcquire()) {
            log.info("rate limited");
            return ResponseEntity.badRequest().body("rate limit, permit 1 request per 10 seconds");
        }

        //获取最近一次的location
        InterestingPoint point = interestingPointRepository.findFirstByUserName(interestingPoint.getUserName());
        GeoHash oldGeoHash = GeoHash.withCharacterPrecision(point.getLat(), point.getLng(), PRECISION_OF_NUM_OF_CHARACTERS);
        GeoHash newGeoHash = GeoHash.withCharacterPrecision(interestingPoint.getLat(), interestingPoint.getLng(), PRECISION_OF_NUM_OF_CHARACTERS);
        String driverName = interestingPoint.getUserName();
        String oldGeoHashBase32 = oldGeoHash.toBase32();
        String newGeoHashBase32 = newGeoHash.toBase32();
        if (!oldGeoHashBase32.equals(newGeoHashBase32)) {

            String oldGeohash6 = prefixOfString(oldGeoHashBase32, 6);
            String oldGeohash5 = prefixOfString(oldGeoHashBase32, 5);
            redisTemplate.opsForSet().remove(oldGeohash6, driverName);
            redisTemplate.opsForSet().remove(oldGeohash5, driverName);

            String newGeohash6 = prefixOfString(newGeoHashBase32, 6);
            String newGeohash5 = prefixOfString(newGeoHashBase32, 5);
            redisTemplate.opsForSet().add(newGeohash6, driverName);
            redisTemplate.opsForSet().add(newGeohash5, driverName);
        } else {
            log.info("the new postion has the same geohash comparing with the recent location");
        }
        return ResponseEntity.ok().build();
    }
    @PostMapping("/trip")
    public ResponseEntity createTrip(@RequestBody Trip trip) {
        trip.setTripId(UUIDs.timeBased());
        trip.setStatus(0);
        tripRepository.save(trip);


        //找到符合条件的几个司机，然后把trip信息发送给对应的司机
        List<InterestingPoint> driversLocations = getNearbyDrivers(trip.getLat(), trip.getLng());
        for (InterestingPoint point : driversLocations) {
            //trip.setDriverUserName(point.getUserName());
            sendTripRequestToDrivers(trip);
        }

        return ResponseEntity.ok().build();
    }

    @PutMapping("/trip")
    public ResponseEntity updateTrip(@RequestBody Trip trip) {
        Trip tripInfo = tripRepository.findById(trip.getTripId()).get();
        if (tripInfo != null) {
            //在设置driver的时候需要用一个分布式锁来锁定，或者有其他方式来实现transaction，防止出现不一致性
            if (!Strings.isNullOrEmpty(tripInfo.getDriverUserName())) {
                log.info("driver is set for the trip");
                return ResponseEntity.badRequest().body("Trip已经被其他司机抢单了");
            } else {
                log.info("driver name is null, we can set the trip");
                tripRepository.save(trip);
                return ResponseEntity.ok().build();
            }
        } else {
            log.error("can't find trip info by tripID");
            return ResponseEntity.badRequest().body("不能根据ID查询到trip信息");
        }
    }

    @GetMapping("/trip")
    public ResponseEntity getTrip(String uuid) {
        Trip tripInfo = tripRepository.findById(UUID.fromString(uuid)).get();
        if (tripInfo != null && !Strings.isNullOrEmpty(tripInfo.getDriverUserName())) {
            log.info("found trip accpted");
            return ResponseEntity.ok().body(tripInfo);
        } else {
            log.warn("trip is not accepted by the driver");
            return ResponseEntity.notFound().build();
        }
    }
    /**
     * 给司机发送请求，等待司机相应是否接受
     * @param trip
     */
    private void sendTripRequestToDrivers(Trip trip) {
        messageService.pushMessage(PUSH_MESSAGE_USERNAME, trip.getDriverUserName(), trip);
    }

    public List<InterestingPoint> getNearbyDrivers(double lat, double lng) {
        GeoHash geoHash = GeoHash.withCharacterPrecision(lat, lng, PRECISION_OF_NUM_OF_CHARACTERS);
        String geohashBase32 = geoHash.toBase32();

        List<InterestingPoint> res = Lists.newArrayList();
        //需要查6位的（0。6公里内），5位的（2.4公里内），4位的（20公里内的）
        String geoHash6 = prefixOfString(geohashBase32, 6);
        Set<String> drivers = redisTemplate.opsForSet().members(geoHash6);

        for (String driver : drivers) {
            InterestingPoint point = interestingPointRepository.findFirstByUserName(driver);
            res.add(point);
        }

        String geoHash5 = prefixOfString(geohashBase32, 5);
        drivers = redisTemplate.opsForSet().members(geoHash5);
        for (String driver : drivers) {
            InterestingPoint point = interestingPointRepository.findFirstByUserName(driver);
            res.add(point);
        }
        //根据业务需求过滤出需要的附近的数据，或者分页返回
        return res.stream().filter(point -> (System.currentTimeMillis() - point.getTime().timestamp()) < 3600 * 1000 ).limit(3).collect(Collectors.toList());
    }

    private String prefixOfString(String str, int length) {
        return str.substring(0, length - 1);
    }
}
