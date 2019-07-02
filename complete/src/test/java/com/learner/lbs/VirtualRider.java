package com.learner.lbs;

import lombok.Data;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Data
@Log4j2
@ToString
public class VirtualRider {
    private String baseUrl = "http://localhost:8080";
    private String userName;
    private RestTemplate restTemplate;
    private double lat;
    private double lng;
    private Trip mytrip;

    public VirtualRider(String userName, double lat, double lng) {
        this.userName = userName;
        this.lat = lat;
        this.lng = lng;
    }

    /**
     * 第一步：创建一个ride请求
     */
    public void sendTripRequest() {
        mytrip = new Trip();
        mytrip.setRiderUserName(userName);
        mytrip.setLat(lat);
        mytrip.setLng(lng);
        String url = String.format("%s/lbs/trip", baseUrl);
        Trip ret = restTemplate.postForObject(url, mytrip, Trip.class);
        log.info(ret);
        mytrip = ret;
        UUID tripId = ret.getTripId();
    }

    /**
     * 第二部：查询是否有司机抢单
     */
    //开始查询附近的司机
    public Trip queryTrip() {
        String url = String.format("%s/lbs/trip?uuid=%s", baseUrl, mytrip.getTripId().toString());

        Trip trip = restTemplate.getForObject(url, Trip.class);
        log.info("trip info: " + trip);
        return trip;
    }

}
