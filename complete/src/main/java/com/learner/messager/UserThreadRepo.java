package com.learner.messager;

import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

public interface UserThreadRepo extends CassandraRepository<UserThread, String> {
    /**
     * 根据userName获取该用户参与的thread
     * @param userName
     * @return
     */
    public List<UserThread> getUserThreadsByUserName(String userName);
}
