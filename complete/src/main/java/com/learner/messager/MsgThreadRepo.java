package com.learner.messager;

import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

public interface MsgThreadRepo extends CassandraRepository<MsgThread, String> {
    public List<MsgThread> findMsgThreadsByUserName(String userName);
    public List<MsgThread> getMsgThreadsByThreadId(UUID uuid);
    public List<MsgThread> getMsgThreadsByThreadIdIn(UUID uuid);

}
