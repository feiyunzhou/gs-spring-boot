package com.learner.lbs;

import com.datastax.driver.core.utils.UUIDs;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

import static org.springframework.data.cassandra.core.cql.Ordering.DESCENDING;
import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;


@Table("intresting_point")
@Data
@ToString
public class InterestingPoint {
    @PrimaryKeyColumn(name = "username", type = PARTITIONED)
    private String userName;
    @PrimaryKeyColumn(name = "time", ordinal = 1, ordering = DESCENDING)
    private UUID time;
    @Column
    private double lat;
    @Column
    private double lng;
/*
    public InterestingPoint(String userName, double lat, double lng) {
        this.userName = userName;
        this.lat = lat;
        this.lng = lng;
    }*/
    public static InterestingPoint createInterestingPoint(String userName, double lat, double lng) {
        InterestingPoint record = new InterestingPoint();
        record.setUserName(userName);
        record.setLat(lat);
        record.setLng(lng);
        record.setTime(UUIDs.timeBased());
        record.setLat(lat);
        record.setLng(lng);
        return record;
    }
}