package com.learner.messager;

import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;

public interface MsgThreadRepo extends CassandraRepository<MsgThread, String> {
    public List<MsgThread> findMsgThreadsByUserName(String userName);
}
