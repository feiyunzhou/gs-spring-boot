package com.learner.feed;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRepository extends CassandraRepository<Friend, String> {
}