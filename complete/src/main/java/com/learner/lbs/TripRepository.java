package com.learner.lbs;

import com.learner.feed.Tweet;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TripRepository extends CassandraRepository<Trip, UUID> {
    public Trip findTripByTripId(UUID uuid);
}