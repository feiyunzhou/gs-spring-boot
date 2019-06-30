package com.learner.lbs;

import ch.hsr.geohash.GeoHash;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

    @PostMapping("/driver/location")
    public ResponseEntity driverLocationReport(@RequestBody Location location, String userName) {
        GeoHash geoHash = GeoHash.withCharacterPrecision(location.getLat(), location.getLng(), PRECISION_OF_NUM_OF_CHARACTERS);
        String geohashBase32 = geoHash.toBase32();
        Map<String, Object> map = Maps.newHashMap();
        map.put(LAT_KEY, location.getLat());
        map.put(LNG_KEY, location.getLng());
        map.put(GEO_HASH_KEY, geohashBase32);

        /**
         * 保存从司机端上传的位置信息
         */
        long ts = System.currentTimeMillis();
        String locationId = location.getUserName() + ":" + ts / 1000;
        redisTemplate.opsForHash().putAll(locationId, map);

        //为精度为5的距离创建索引，这样才查询的时候可以使用这个数据来查询附近的地址。附近的地址通常由这个值来决定，相近的位置通常北聚合在这个set中。
        //问题： 如果这个set的数据量很大？会如何？
        String geohash6 = prefixOfString(geohashBase32, 6);
        redisTemplate.opsForSet().add(geohash6, locationId);

        String geohash5 = prefixOfString(geohashBase32, 5);
        redisTemplate.opsForSet().add(geohash5, locationId);

        String geohash4 = prefixOfString(geohashBase32, 4);
        redisTemplate.opsForSet().add(geohash4, locationId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/nearyby")
    public List<Location> nearby(@RequestBody Location location, String userName) {
        GeoHash geoHash = GeoHash.withCharacterPrecision(location.getLat(), location.getLng(), PRECISION_OF_NUM_OF_CHARACTERS);
        String geohashBase32 = geoHash.toBase32();

        //需要查6位的（0。6公里内），5位的（2.4公里内），4位的（20公里内的）
        String geoHash5 = prefixOfString(geohashBase32, 5);
        Set<String> locations = redisTemplate.opsForSet().members(geoHash5);

        List<Location> res = Lists.newArrayList();
        for (String loc : locations) {
            Map<String, Object> map = redisTemplate.opsForHash().entries(loc);
            res.add(new Location(loc, (double) map.get(LAT_KEY), (double) map.get(LNG_KEY)));
        }

        //根据业务需求过滤出需要的附近的数据，或者分页返回
        return res;
    }

    
    private String prefixOfString(String str, int length) {
        return str.substring(0, length - 1);
    }
}
