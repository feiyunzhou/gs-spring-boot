package com.learner.feed;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimelineRepository extends CassandraRepository<TimelineRecord, String> {
}