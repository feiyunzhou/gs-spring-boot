package com.learner.lbs;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;
import java.util.UUID;

@Table("trip")
@Data
@ToString
public class Trip {
    @Column("trip_id")
    private UUID tripId;
    @Column("driver_username")
    private String driverUserName;
    @Column("rider_username")
    private String riderUserName;
    @Column
    private double lat;
    @Column
    private double lng;
    @Column("status")
    private int status;
    @Column("create_time")
    private Date createTime;
}
