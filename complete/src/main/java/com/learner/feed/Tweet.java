package com.learner.feed;

import com.datastax.driver.core.utils.UUIDs;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;
import java.util.UUID;

import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

@Table("tweet")
@Data
@ToString
public class Tweet {
    @PrimaryKeyColumn(name = "tweet_id", type = PARTITIONED)
    private UUID tweetId;
    @Column("username")
    private String userName;
    @Column
    private String body;
    @Transient
    private Date date;

    public static Tweet createTweet(String owner, String body) {
        Tweet tweet = new Tweet();
        tweet.setTweetId(UUIDs.timeBased());
        tweet.setUserName(owner);
        tweet.setBody(body);
        return tweet;
    }
}
