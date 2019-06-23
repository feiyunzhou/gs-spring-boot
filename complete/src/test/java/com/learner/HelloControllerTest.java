package com.learner;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learner.messager.*;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations="classpath:application.properties")
@Log4j2
public class HelloControllerTest {

    @Autowired
    private MockMvc mvc;
    //@Autowired
    //PersonRepository personRepository;
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    InboxRepository inboxRepository;
    @Autowired
    InboxMessageRepository inboxMessageRepository;

    @Test
    public void getHello() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Greetings from Spring Boot!")));
    }

    @Test
    public void msgTest() throws Exception {
        Message msg = Message.createMessage("tester5", "hello world, from tester5");
        Message message = messageRepository.save(msg);
        log.info(message);
    }
    @Test
    public void inboxTest() throws Exception {
        UUID timeuuid =  UUID.fromString("a3ba69b0-93fe-11e9-bf65-d97e4ccad42a");
        List<Inbox> inboxes = inboxRepository.getInboxesByKey_UsernameAndKey_TimeGreaterThan("test2", timeuuid);
        inboxes.forEach(inbox -> {
            log.info(inbox.getKey().getUsername());
        });
    }
    @Test
    public void msgQueryTest() throws Exception {
        Message msg = messageRepository.findById(UUID.fromString("3b831162-2a98-4d92-af7b-d7d3a040a862")).get();
        log.info(msg);
    }
    @Test
    public void insertInbox() throws Exception {
        //InboxKey key = InboxKey.createKey("tester1");
        //Message msg = Message.createMessage("tester8", "hello world, from tester8");
        //messageRepository.save(msg);

        Message msg = messageRepository.findById(UUID.fromString("3b831162-2a98-4d92-af7b-d7d3a040a862")).get();

        if (msg != null) {
            Inbox inbox = Inbox.createInboxMessage(msg.getMessageId(), false, "test15", msg.getUserName());
            inboxRepository.save(inbox);
        }
    }

    @Test
    public void inboxQuery() throws Exception {
        UUID timeuuid =  UUID.fromString("a3ba69b0-93fe-11e9-bf65-d97e4ccad42a");
        List<Inbox> inboxes = inboxRepository.findInboxesByKey_UsernameAndKey_TimeGreaterThan("test2", timeuuid);
        inboxes.forEach(b -> System.out.println(b));
        //inboxes.stream().forEach(System.out::println);
    }

    @Test
    public void insertInboxMessageTest() throws Exception {
        InboxMessage msg = InboxMessage.createInboxMessage("test1", "hello world 199", "test51");
        inboxMessageRepository.save(msg);
    }

    @Test
    public void inboxQueryTest() throws Exception {
        List<InboxMessage> msgs = inboxMessageRepository.getInboxMessagesByToAndTimeGreaterThan("test1", UUID.fromString("040dd790-943e-11e9-af45-0fbd8af4f681"));
        msgs.forEach(log::info);
    }
}