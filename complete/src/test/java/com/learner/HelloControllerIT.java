package com.learner;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.datastax.driver.core.utils.UUIDs;
import com.google.gson.Gson;
import com.learner.messager.InboxMessage;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Log4j2
public class HelloControllerIT {

    @LocalServerPort
    private int port;

    private URL base;

    @Autowired
    private TestRestTemplate template;

    @Before
    public void setUp() throws Exception {
        this.base = new URL("http://localhost:" + port + "/");
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
        List<Map> messages  = template.getForObject(base.toString() + String.format("msg?userName=%s&uuid=", userName, lastUUid.toString()), List.class);
        Gson gson = new Gson();
        for (Map map : messages) {
            InboxMessage msg = gson.fromJson(gson.toJson(map), InboxMessage.class);
            log.info(msg);
            lastUUid = msg.getTime();
        }

        while (true) {
            Object obj = template.getForObject(base.toString() + String.format("connect?userName=%s",userName), Object.class);
            log.info("we connected:" + obj);
            System.out.println("disconnected and keep connected");
            messages = template.getForObject(base.toString() + String.format("msg?userName=%s&uuid=", userName, lastUUid.toString()), List.class);
            for (Map map : messages) {
                InboxMessage msg = gson.fromJson(gson.toJson(map), InboxMessage.class);
                log.info("received msg:" + msg.toString());
            }
        }
    }

    @Test
    public void pushAmessage() throws Exception {
        final String userName = "test1";
        InboxMessage inboxMessage = InboxMessage.createInboxMessage("test1" , "i am test2, how are you!", "test2");
        template.postForEntity(base.toString() + String.format("msg"), inboxMessage, InboxMessage.class);

    }
}
