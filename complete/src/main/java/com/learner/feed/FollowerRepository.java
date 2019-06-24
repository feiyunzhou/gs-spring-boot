package com.learner.feed;

import com.learner.messager.InboxMessage;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FollowerRepository extends CassandraRepository<Follower, String> {
    List<Follower> findFollowersByUserName(String userName);
    List<Follower> findFollowersByUserNameAndFollower();
}