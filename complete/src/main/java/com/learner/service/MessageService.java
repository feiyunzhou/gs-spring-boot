package com.learner.service;

import com.google.gson.Gson;
import com.learner.messager.InboxMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MessageService {
    @Autowired
    private RestTemplate restTemplate;

    private String baseUrl = "http://localhost:8080";
    public void pushMessage(String fromUser, String toUser, Object messageData) {
        Gson gson = new Gson();
        String body = gson.toJson(messageData);

        String url = String.format("%s/ms/msg", baseUrl);
        InboxMessage message = new InboxMessage();
        message.setFrom(fromUser);
        message.setTo(toUser);
        message.setMsg(body);
        restTemplate.postForObject(url, message, InboxMessage.class);
    }
}
