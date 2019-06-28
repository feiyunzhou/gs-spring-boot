package com.learner.feed;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TweetRepository extends CassandraRepository<Tweet, UUID> {
    List<Tweet> findAllByTweetIdIn(List<UUID> uuids);
}