package com.learner.feed;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;

import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

@Table("friend")
@Data
public class Friend {
    @PrimaryKeyColumn(name = "username", type = PARTITIONED)
    private String userName;
    @PrimaryKeyColumn(name = "friend", ordinal = 1)
    private String friend;
    @Column
    private Date since;

    public static Friend createObject(String userName, String friendName) {
        Friend friend = new Friend();
        friend.setFriend(friendName);
        friend.setUserName(userName);
        friend.setSince(new Date());
        return friend;
    }
}
