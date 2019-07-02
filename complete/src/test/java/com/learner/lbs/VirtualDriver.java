package com.learner.lbs;

import com.datastax.driver.core.utils.UUIDs;
import com.google.gson.Gson;
import com.learner.messager.InboxMessage;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Data
@Log4j2
public class VirtualDriver {
    private String baseUrl = "http://localhost:8080";
    private String name;
    private RestTemplate restTemplate;
    double lat;
    double lng;
    public VirtualDriver(String name){
        this.name = name;
        // driver's initial lat and lng:  39.950990, 116.388493
        lat = 39.950990;
        lng =  116.388493;
    }
    public VirtualDriver(String name, double lat, double lng){
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }
    public void run() {
        //report;
        /**
         * 每隔10秒上报位置
         */
        new Thread(() -> {
            while (true) {
                try {
                    double randomLat = 0.000001 * (Math.random() * 500.0d + 1);
                    double randomlng = 0.000001 * (Math.random() * 1000.0d + 1);
                    double tempLat = lat + randomLat;
                    double tmpLng = lng + randomlng;
                    String url = String.format("%s/lbs/driver/location", baseUrl);
                    log.info("ready to post data:" + url);
                    InterestingPoint point = new InterestingPoint();
                    point.setUserName(name);
                    point.setLat(tempLat);
                    point.setLng(tmpLng);
                    ResponseEntity<Object> resp = restTemplate.postForEntity(url, point, Object.class);
                    log.info(resp);
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        /**
         *  启动消息通道
         */
        new Thread(() -> {
            try {
                startMsgChannel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void handleMsg(InboxMessage msg) {
        log.info("handle msg:" + msg);
    }
    private void startMsgChannel() throws Exception {
        final String userName = name;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        UUID lastUUid = UUIDs.startOf( sdf.parse("2012-05-03").getTime());
        String url = String.format("%s/ms/msg?userName=%s&uuid=%s",baseUrl, userName, lastUUid.toString());
        log.info(url);
        List<Map> messages  = restTemplate.getForObject(url, List.class);
        Gson gson = new Gson();
        int i = 0;
        for (Map map : messages) {
            InboxMessage msg = gson.fromJson(gson.toJson(map), InboxMessage.class);
            handleMsg(msg);
            if (i == 0)  lastUUid = msg.getTime();
            i++;
        }

        while (true) {
            Object obj = restTemplate.getForEntity(String.format("%s/ms/connect?userName=%s", baseUrl, userName), Object.class);
            log.info("we connected:" + obj);
            System.out.println("disconnected and keep connected");
            url = String.format("%s/ms/msg?userName=%s&uuid=%s", baseUrl, userName, lastUUid.toString());
            messages = restTemplate.getForObject(url, List.class);
            i = 0;
            for (Map map : messages) {
                InboxMessage msg = gson.fromJson(gson.toJson(map), InboxMessage.class);
                if (i == 0)  lastUUid = msg.getTime();
                i++;
                handleMsg(msg);
                log.info("received msg:" + msg.toString());
            }
        }
    }
}
