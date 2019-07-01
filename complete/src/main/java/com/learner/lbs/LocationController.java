package com.learner.lbs;

import ch.hsr.geohash.GeoHash;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *   1. 司机位置信息上传到Location 表 username_createtime -> [lat,lng,geohash]，司机每隔3秒更新位置
 *      1M司机/3s = 300k/s QPS
 *
 *   2. 根据位置更新信息为geohash建立索引，只取geohash的前5位作为索引，可以根据geohash查到对应的司机的locationId，也就是geohash->[locationId1, locationId2]
 *   3. 使用Redis， Redis的QPS能到 > 100k QPS
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

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private InterestingPointRepository interestingPointRepository;

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
    @PostMapping("/trip-req")
    public ResponseEntity createTrip(@RequestBody Trip trip) {
        trip.setTripId(UUIDs.timeBased());
        trip.setStatus(0);
        List<InterestingPoint> driversPoints = getNearbyDrivers(trip.getLat(), trip.getLng());

        List<InterestingPoint> driverLocations = getNearbyDrivers(trip.getLat(), trip.getLng());
        return ResponseEntity.ok().build();
    }

    /**
     * 给司机发送请求，等待司机相应是否接受
     * @param driverLocations
     */
    private void sendTripRequestToDrivers(List<InterestingPoint> driverLocations) {

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
