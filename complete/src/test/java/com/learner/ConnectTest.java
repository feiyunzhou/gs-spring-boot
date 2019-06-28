package com.learner;

import com.datastax.driver.core.utils.UUIDs;
import com.google.gson.Gson;
import com.learner.messager.InboxMessage;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Log4j2
public class ConnectTest {

    @LocalServerPort
    private int port;

    private URL base;

    @Autowired
    private RestTemplate template;

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource(name="redisTemplate")
    private HashOperations hashOperations;

    @Resource(name="redisTemplate")
    private ZSetOperations zSetOperations;

    @Before
    public void setUp() throws Exception {
        this.base = new URL("http://localhost:" + 8080 + "/");
    }

    @Test
    public void getHello() throws Exception {
        ResponseEntity<String> response = template.getForEntity(base.toString(),
                String.class);
        assertThat(response.getBody(), equalTo("Greetings from Spring Boot!"));
    }

    @Test
    public void connectTest2() throws Exception {
        final String userName = "test1";
        Object obj = template.getForObject(base.toString() + String.format("connect?userName=%s",userName), Object.class);
        log.info("we connected:" + obj);
    }
    @Test
    public void connectTest() throws Exception {
        final String userName = "test1";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        UUID lastUUid = UUIDs.startOf( sdf.parse("2012-05-03").getTime());
        String url = base.toString() + String.format("msg?userName=%s&uuid=%s", userName, lastUUid.toString());
        log.info(url);
        List<Map> messages  = template.getForObject(url, List.class);
        Gson gson = new Gson();
        int i = 0;
        for (Map map : messages) {
            InboxMessage msg = gson.fromJson(gson.toJson(map), InboxMessage.class);
            log.info(msg);
            if (i == 0)  lastUUid = msg.getTime();
            i++;
        }

        while (true) {
            Object obj = template.getForEntity(base.toString() + String.format("connect?userName=%s",userName), Object.class);
            log.info("we connected:" + obj);
            System.out.println("disconnected and keep connected");
            url = base.toString() + String.format("msg?userName=%s&uuid=%s", userName, lastUUid.toString());
            messages = template.getForObject(url, List.class);
            i = 0;
            for (Map map : messages) {
                InboxMessage msg = gson.fromJson(gson.toJson(map), InboxMessage.class);
                if (i == 0)  lastUUid = msg.getTime();
                i++;
                log.info("received msg:" + msg.toString());
            }
        }
    }

    @Test
    public void pushAmessage() throws Exception {
        final String userName = "test1";
        InboxMessage inboxMessage = InboxMessage.createInboxMessage("test1" , "so buggy!", "test2");
        template.postForEntity(base.toString() + String.format("msg"), inboxMessage, InboxMessage.class);
    }

    @Test
    public void test1(){
        redisTemplate.opsForValue().set("my","my");
        Assert.assertEquals("my",redisTemplate.opsForValue().get("my"));
    }
    @Test
    public void test2() {
        Map<String, String> map = new HashMap<>();
        map.put("age", "13");
        map.put("name", "selrain");
        hashOperations.putAll("test2", map);

        Assert.assertEquals("13",hashOperations.get("test2","age"));

        hashOperations.delete("test2","age","name");
    }

    @Test
    public void test3(){
        zSetOperations.add("selrain","selraion",1);
        Assert.assertEquals("selraion",zSetOperations.range("selrain",0,1).iterator().next());
        zSetOperations.remove("selrain","selraion");
    }
    @Test
    public void test4(){
        redisTemplate.execute((RedisConnection connection)->{
            Jedis jedis=(Jedis)connection.getNativeConnection();
            String s=jedis.set("test4","selrain","NX","EX",6000);
            return s;
        });
    }
}
