package com.learner.feed;

import com.datastax.driver.core.utils.UUIDs;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

import static org.springframework.data.cassandra.core.cql.Ordering.DESCENDING;
import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

@Table("timeline")
@Data
@ToString
public class TimelineRecord {
    @PrimaryKeyColumn(name = "username", type = PARTITIONED)
    private String userName;
    @PrimaryKeyColumn(name = "time", ordinal = 1, ordering = DESCENDING)
    private UUID time;
    @Column("tweet_id")
    private UUID tweetId;

    public static TimelineRecord createTimelineRec(String userName, UUID tweetId) {
        TimelineRecord record = new TimelineRecord();
        record.setUserName(userName);
        record.setTweetId(tweetId);
        record.setTime(UUIDs.timeBased());
        return record;
    }
}
