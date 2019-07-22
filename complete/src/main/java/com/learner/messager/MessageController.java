package com.learner.messager;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.learner.Person;
import com.learner.PersonKey;
import com.learner.PersonRepository;
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
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 *   messenger实现记录
 *   1. 消息的处理
 *      1）采用longpolling的方式, timeout 设置为30秒左右比较合适，可以看这里： https://tools.ietf.org/id/draft-loreto-http-bidirectional-07.html#timeouts
 *      2）使用了springde DeferredResult，实际也就是NIO技术，异步的方式处理请求，只有一个线程处理I/O，这样可以支持大量的请求，例如可以每台服务器可以支持5万个请求
 *      3）需要用一张表来跟踪当前哪天服务器连接着用户。
 *   2. 数据的保存
 *       1）将数据保存在cassandra中，设计了一个模型，也就是InboxMessage, 也就是用户的收到的消息。以userName作为partition，创建时间作为某个节点上数据的索引。 可以把这个表看为一个map，
 *          key是userName, value是sortedMap， 这个sortedMap中key是排序的时间，值是消息的内容。通过这种方式，可以快速获取某个用户获得的内容，但坏处是，如果某个用户的消息特别多的时候，这个数据量会比较大，但我们可以
 *          采用其他方式来避免这个问题 a)对userName再做分区。b)设置消息的有效时间。一定时间的数据有TTL
 *
 *
 *   3. 用户状态的处理
 *        1）可以在用户上线的时候拉取数据，而且是用户的比较活跃的好友的状态。不停的去查询状态
 *        2）可以在发送消息的时候更新数据
 *        3）只有用户上线的时候，通知他的好友他上线了的状态。而不是不断的跟踪状态，否则会消耗很多资源
 *
 *   4. 使用Thread来管理用户聊天会话，这样可以实现群组聊天，或是实现channel（channel更偏重于动态的用户）
 *       1）当用户创建和某人聊天时候需要创建一个thread， thread保存了参与者的信息
 *       2）用户需要拉取创建的会话信息，从而可以获取会话的参与者信息和会话ID
 *       3） 用户向thread ID发送消息，在此会话中的用户都会收到相应的消息
 *
 */
@RestController
@Log4j2
@RequestMapping("/ms")
public class MessageController {
    @Autowired
    private PersonRepository personRepository;
    private final Map deferredResultMap=new ConcurrentReferenceHashMap<>();
    @Autowired
    private InboxMessageRepository inboxMessageRepository;
    @Autowired
    private MsgThreadRepo msgThreadRepo;
    @Autowired
    private UserThreadRepo userThreadRepo;

    @GetMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }
    @GetMapping("/connect")
    public DeferredResult longPolling(HttpServletRequest req) throws Exception {
        String userName = req.getParameter("userName");
        if (Strings.isNullOrEmpty(userName)) {
            throw new Exception("invalid username");
        }
        log.info(String.format("user:%s started a long polling", userName));
        if (deferredResultMap.containsKey(userName)) {
            deferredResultMap.remove(userName);
        }
        DeferredResult deferredResult=new DeferredResult(10000L);
        deferredResultMap.put(userName, deferredResult);
        log.info(deferredResultMap);
        deferredResult.onCompletion(()->{
            deferredResultMap.remove(userName);
            System.err.println("还剩"+deferredResultMap.size()+"个deferredResult未响应");
        });
        deferredResult.onTimeout(() -> {
            deferredResultMap.remove(userName);
            log.error("timeout issues");
            Gson gson = new Gson();
            deferredResult.setErrorResult(gson.toJson("timing"));
        });

        deferredResult.onError(error -> {
            log.error("error happened", error);
            deferredResultMap.remove(userName);
        });
        return deferredResult;
    }

    @PostMapping("/msg")
    public void returnLongPollingValue(HttpServletRequest req, @RequestBody InboxMessage msg){
        //save message to the inbox
        msg.setTime(UUIDs.timeBased());
        msg.setMessageType(MessageType.TEXT);
        inboxMessageRepository.save(msg);

        if (deferredResultMap.containsKey(msg.getTo())) {
            DeferredResult defRes = (DeferredResult) deferredResultMap.get(msg.getTo());
            Gson gson = new Gson();
            defRes.setResult(gson.toJson("new msg"));
        }
        log.info(deferredResultMap);
        //defRes.setResult(ResponseEntity.ok().body("I receive msg from " + from));
    }

    /**
     * Post消息得时候需要post到某个thread中去，而不是发送到某个用户中去
     * @param req
     * @param msg
     * @return
     * @throws Exception
     */
    @PostMapping("/thread/msg")
    public ResponseEntity postMsgToThread(HttpServletRequest req, @RequestBody InboxMessage msg) throws Exception {
        UUID threadId =  msg.getThreadId();
        MsgThread threadInfo = msgThreadRepo.getMsgThreadsByThreadId(threadId);

        if (threadInfo == null) {
            return ResponseEntity.badRequest().body("threadId is invalid");
        }
        if (!threadInfo.getParticipants().contains(msg.getFrom())) {
            return ResponseEntity.badRequest().body("the user is not in this thread, and not allow to send the msg");
        }

        Iterator<String> it = threadInfo.getParticipants().iterator();
        int count = 0;
        while (it.hasNext()) {
            String to = it.next();
            if (to.equalsIgnoreCase(msg.getFrom())) continue;

            InboxMessage deliveredMsg = InboxMessage.createInboxMessage(to, msg.getMsg(), msg.getFrom());
            deliveredMsg.setTime(UUIDs.timeBased());
            deliveredMsg.setMessageType(MessageType.TEXT);
            deliveredMsg.setThreadId(msg.getThreadId());
            /**
             * @ TODO
             * 如果群聊的数量特别多的时候，这里是一个瓶颈，需要分批发送消息。应该放到队列中，由队列去处理
             */
            inboxMessageRepository.save(deliveredMsg);
            if (deferredResultMap.containsKey(to)) {
                DeferredResult defRes = (DeferredResult) deferredResultMap.get(deliveredMsg.getTo());
                Gson gson = new Gson();
                defRes.setResult(gson.toJson("new msg"));
            }
            count++;
        }
        log.info(deferredResultMap);
        return ResponseEntity.ok(count);
    }
    /**
     * participants中的第一个用户就是创建thread的owner
     * 是否创建thread由客户端来决定。
     * @param msgThread
     */
    @PostMapping("/thread")
    public MsgThread createThread(@RequestBody MsgThread msgThread) {
        //List<UserThread> threads = userThreadRepo.getUserThreadsByUserName(msgThread.getOwner());
        UUID threadId = UUID.randomUUID();
        msgThread.setThreadId(threadId);
        msgThread.getParticipants().add(msgThread.getOwner());
        msgThread.setCreateTime(new Date());
        msgThread.setModifyTime(new Date());
        msgThread.setThreadType(0);
        MsgThread thread = msgThreadRepo.save(msgThread);

        for (String u : msgThread.getParticipants()) {
            UserThread userThread = new UserThread();
            userThread.setThreadId(threadId);
            userThread.setUserName(u);
            userThreadRepo.save(userThread);
        }
        return thread;
    }

    /**
     * 将用户添加进thread，使用MsgThread作为对象传递参数：使用threadId和participants
     * @param msgThread
     * @return
     */
    @PutMapping("/thread/add-user")
    public MsgThread addUserIntoThread(@RequestBody MsgThread msgThread) {
        MsgThread threadInfo = msgThreadRepo.getMsgThreadsByThreadId(msgThread.getThreadId());
        Set<String> newUsers = msgThread.getParticipants();

        Set<String> olderUsers = threadInfo.getParticipants();

        List<String> addings = newUsers.stream().filter(u -> !olderUsers.contains(u)).collect(Collectors.toList());

        //为用户建立索引，需要删除
        for (String u : addings) {
            UserThread userThread = new UserThread();
            userThread.setThreadId(threadInfo.getThreadId());
            userThread.setUserName(u);
            threadInfo.getParticipants().add(u);
            userThreadRepo.save(userThread);
            olderUsers.add(u);
        }
        MsgThread res = msgThreadRepo.save(threadInfo);
        return res;
    }

    /**
     * 把用户从thread中删除出去
     * @param msgThread
     * @return
     */
    @PutMapping("/thread/rm-user")
    public MsgThread removeUserFromThread(@RequestBody MsgThread msgThread) {
        MsgThread threadInfo = msgThreadRepo.getMsgThreadsByThreadId(msgThread.getThreadId());
        Set<String> rmUsers = msgThread.getParticipants();

        Set<String> currentUsers = threadInfo.getParticipants();

        List<String> removings = rmUsers.stream().filter(u -> currentUsers.contains(u)).collect(Collectors.toList());

        //为用户建立索引，需要删除
        for (String u : removings) {
            UserThread userThread = new UserThread();
            userThread.setThreadId(threadInfo.getThreadId());
            userThread.setUserName(u);
            userThreadRepo.delete(userThread);
            currentUsers.remove(u);
        }
        MsgThread res = msgThreadRepo.save(threadInfo);
        return res;
    }
    @GetMapping("/thread")
    public List<MsgThread> fetchThreadsByUser(String userName) {
        List<UserThread> threadIds = userThreadRepo.getUserThreadsByUserName(userName);
        List<UUID> ids = threadIds.stream().map(t -> t.getThreadId()).collect(Collectors.toList());
        List<MsgThread> threads = msgThreadRepo.getMsgThreadsByThreadIdIn(ids);
        threads.stream().forEach(t -> {
            List<String> parts = t.getParticipants().stream().filter(u -> !u.equalsIgnoreCase(userName)).collect(Collectors.toList());
            t.setName(Joiner.on(",").join(parts));
        });
        return threads;
    }

    @GetMapping("/thread/{id}")
    public MsgThread fetchThreadsByUser(String userName, @PathVariable("id") String id) {
        MsgThread thread = msgThreadRepo.getMsgThreadsByThreadId(UUID.fromString(id));
        thread.setName(Joiner.on(",").join(thread.getParticipants().stream().filter(u -> !u.equalsIgnoreCase(userName)).collect(Collectors.toList())));
        return thread;
    }
    @GetMapping("/msg")
    public List<InboxMessage> fetchMessageByUser(HttpServletRequest req, String userName, String uuid) throws Exception {
        log.info(String.format("met msg for username:%s, uuid:%s", userName, uuid));
        List<InboxMessage> inboxes = Lists.newArrayList();
        if (uuid != null && !uuid.isEmpty()) {
            inboxes = inboxMessageRepository.getInboxMessagesByToAndTimeGreaterThan(userName, UUID.fromString(uuid));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            inboxes = inboxMessageRepository.getInboxMessagesByToAndTimeGreaterThan(userName, UUIDs.startOf(sdf.parse("20120503").getTime()));
        }
        return inboxes;
    }

    @GetMapping("/online-users")
    public List<String> fetchOnlineUsers(HttpServletRequest req) {
        if(deferredResultMap.keySet() != null) {
            return Lists.newArrayList(deferredResultMap.keySet());
        }
        return Lists.newArrayList();
    }
    @ExceptionHandler
    public void exceptionHandler(HttpServletRequest req, Exception ex) {
        //ex.printStackTrace();
        String userName = req.getParameter("userName");
        log.info("userName:" + userName);
        log.error("failed exception", ex);
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
