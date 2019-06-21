package hello;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@RestController
@Log4j2
public class HelloController {

    @Autowired
    PersonRepository personRepository;
    final Map deferredResultMap=new ConcurrentReferenceHashMap<>();
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    InboxRepository inboxRepository;

    @GetMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }
    @PostMapping("/person")
    public ResponseEntity newperson() {
        String firstName = generateString(new Random(), "ABCDEFGHIJKLMNOP", 5);
        String lastname = generateString(new Random(), "ABCDEFGHIJKLMNOP", 5);
        UUID uuid = UUID.randomUUID();
        PersonKey pk = new PersonKey(firstName, LocalDateTime.now(), uuid);
        Person person = new Person(pk, lastname, 3000.0d);

        personRepository.insert(person);
        return ResponseEntity.ok(uuid.toString() + ":::" + firstName + " " + lastname);
    }

    @GetMapping("/all")
    public DeferredResult<ResponseEntity<?>> handleReqDefResult(Model model) {
        log.info("Received async-deferredresult request");
        DeferredResult<ResponseEntity<?>> output = new DeferredResult<>();

        ForkJoinPool.commonPool().submit(() -> {
            log.info("Processing in separate thread");
            List<Person> personList = Lists.newArrayList();
            try {
                personList = personRepository.findAll();
                Thread.sleep(5);
            } catch (InterruptedException e) {
            }
            log.info("response ok!");
            output.setResult(ResponseEntity.ok().body(personList));
        });

        log.info("servlet thread freed1111");
        return output;
    }

   /* @GetMapping("/connect")
    public DeferredResult<ResponseEntity<?>> connect(Model model) {
        log.info(" request");
        DeferredResult<ResponseEntity<?>> output = new DeferredResult<>();

        ForkJoinPool.commonPool().submit(() -> {
            log.info("Processing in separate thread");
            try {
                Thread.sleep(500000);
            } catch (InterruptedException e) {
            }
            log.info("response ok!");
            output.setResult(ResponseEntity.ok().build());
        });

        log.info("servlet thread freed1111");
        output.onTimeout(new Runnable() {
            @Override
            public void run() {
                log.error("timing out happened");
            }
        });
        return output;
    }*/

    @GetMapping("/connect")
    public DeferredResult<ResponseEntity<?>>  longPolling(HttpServletRequest req){
        String userName = req.getParameter("userName");
        log.info(String.format("user:%s started a long polling", userName));
        if (deferredResultMap.containsKey(userName)) {
            deferredResultMap.remove(userName);
        }
        DeferredResult<ResponseEntity<?>>  deferredResult=new DeferredResult(90000L);
        deferredResultMap.put(userName, deferredResult);
        log.info(deferredResultMap);
        deferredResult.onCompletion(()->{
            deferredResultMap.remove(userName);
            System.err.println("还剩"+deferredResultMap.size()+"个deferredResult未响应");
        });
        deferredResult.onTimeout(() -> {
            deferredResultMap.remove(userName);
            log.error("timeout issues");
        });

        deferredResult.onError(error -> {
            log.error("error happened", error);
            deferredResultMap.remove(userName);
        });
        return deferredResult;
    }

    @PostMapping("/msg")
    public void returnLongPollingValue(HttpServletRequest req, @RequestBody Message msg){
        //save message to the inbox
        Message messageWithId = Message.createMessage(msg.getUserName(), msg.getBody());
        Message msginrepo = messageRepository.save(messageWithId);
        Inbox inbox = Inbox.createInboxMessage(msginrepo.getMessageId(), false, msg.getTo(), msg.getUserName());
        inboxRepository.save(inbox);

        if (deferredResultMap.containsKey(msg.getTo())) {
            DeferredResult defRes = (DeferredResult) deferredResultMap.get(msg.getTo());
            defRes.setResult(ResponseEntity.ok().body("sending message from: " + msg.getUserName() + " to: " + msg.getTo()));
        }
        log.info(deferredResultMap);
        //defRes.setResult(ResponseEntity.ok().body("I receive msg from " + from));
    }

    @GetMapping("/msg")
    public List<Message> fetchMessageByUser(HttpServletRequest req, String userName, String uuid) throws Exception {
        log.info(String.format("met msg for username:%s, uuid:%s", userName, uuid));
        List<Inbox> inboxes = Lists.newArrayList();
        if (uuid != null && !uuid.isEmpty()) {
            inboxes = inboxRepository.findInboxesByKey_UsernameAndKey_TimeGreaterThan(userName, UUID.fromString(uuid));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            inboxes = inboxRepository.findInboxesByKey_UsernameAndKey_TimeGreaterThan(userName, UUIDs.startOf(sdf.parse("20120503").getTime()));
        }
        List<UUID> messageIds = Lists.newArrayList();
        inboxes.forEach(box -> messageIds.add(box.getMessageId()));
        return messageRepository.findAllById(messageIds);
    }
    @ExceptionHandler
    public void exceptionHandler(HttpServletRequest req, Exception ex) {
        //ex.printStackTrace();
        String userName = req.getParameter("userName");
        log.info("userName:" + userName);
        log.error("failed exception", ex);
    }
    public static String generateString(Random rng, String characters, int length)
    {
        char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
    
}
