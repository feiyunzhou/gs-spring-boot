package com.learner.lbs;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;
import java.util.UUID;

import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

@Table("trip")
@Data
@ToString
public class Trip {
    @PrimaryKeyColumn(name = "trip_id", type = PARTITIONED)
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
