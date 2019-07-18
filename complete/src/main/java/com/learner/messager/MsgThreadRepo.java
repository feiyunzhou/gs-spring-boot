package com.learner.messager;

import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

public interface MsgThreadRepo extends CassandraRepository<MsgThread, UUID> {
    public MsgThread getMsgThreadsByThreadId(UUID uuid);
    public List<MsgThread> getMsgThreadsByThreadIdIn(List<UUID> ids);
}
