package com.learner.lbs;

import ch.hsr.geohash.GeoHash;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *   1. 司机位置信息上传到Location 表 username_createtime -> [lat,lng,geohash]，司机每隔3秒更新位置
 *      1M司机/3s = 300k/s QPS
 *
 *   2. 根据位置更新信息为geohash建立索引，只取geohash的前5位作为索引，可以根据geohash查到对应的司机的locationId，也就是geohash->[locationId1, locationId2]
 *
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

    @PostMapping("/driver/location")
    public ResponseEntity driverLocationReport(@RequestBody Location location, String userName) {
        GeoHash geoHash = GeoHash.withCharacterPrecision(location.getLat(), location.getLng(), PRECISION_OF_NUM_OF_CHARACTERS);
        return ResponseEntity.ok().build();
    }

    private String prefixOfString(String str, int length) {
        return str.substring(0, length - 1);
    }
}
