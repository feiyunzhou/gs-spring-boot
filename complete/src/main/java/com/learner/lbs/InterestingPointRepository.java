package com.learner.lbs;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterestingPointRepository extends CassandraRepository<InterestingPoint, String> {
    public InterestingPoint findFirstByUserName(String username);
}