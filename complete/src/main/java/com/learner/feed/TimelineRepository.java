package com.learner.feed;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimelineRepository extends CassandraRepository<TimelineRecord, String> {
    public List<TimelineRecord> getTimelineRecordByUserNameAndTimeGreaterThan(String userName, UUID time);
    public Slice<TimelineRecord> findTimelineRecordsByUserNameAndTimeGreaterThan(String userName, UUID time, Pageable pageable);
    Slice<TimelineRecord> findAllByUserName(String userName, Pageable pageable);
}