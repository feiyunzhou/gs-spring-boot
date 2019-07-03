package com.learner.lbs;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterestingPointRepository extends CassandraRepository<InterestingPoint, String> {
    public InterestingPoint findFirstByUserName(String username);
    public List<InterestingPoint> getInterestingPointsByUserNameAndTimeGreaterThan(String username, UUID lastTime);
}