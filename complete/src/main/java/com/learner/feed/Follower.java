package com.learner.feed;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.sql.Timestamp;
import java.util.Date;

import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

@Table("follower")
@Data
public class Follower {
    @PrimaryKeyColumn(name = "username", type = PARTITIONED)
    private String userName;
    @PrimaryKeyColumn(name = "follower", ordinal = 1)
    private String follower;
    @Column
    private Date since;

    public static Follower createObject(String userName, String followerName) {
        Follower follower = new Follower();
        follower.setFollower(followerName);
        follower.setUserName(userName);
        follower.setSince(new Date());
        return follower;
    }
}
