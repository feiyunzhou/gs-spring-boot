package com.learner.messager;

import com.datastax.driver.core.utils.UUIDs;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.io.Serializable;
import java.util.UUID;

import static org.springframework.data.cassandra.core.cql.Ordering.DESCENDING;
import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

@Table("inbox_message")
@Data
@ToString
public class InboxMessage implements Serializable {
    @PrimaryKeyColumn(name = "to_user", type = PARTITIONED)
    private String to;
    @PrimaryKeyColumn(name = "time", ordinal = 1, ordering = DESCENDING)
    private UUID time;
    @Column
    private String msg;
    @Column("from_user")
    private String from;
    @Column("message_type")
    private MessageType messageType;

    public InboxMessage() {}
    public InboxMessage(String to, UUID time, String msg, String from) {
        this.to = to;
        this.msg = msg;
        this.from = from;
        this.time = time;
    }

    public static InboxMessage createInboxMessage(String to, String msg, String from) {
        UUID uuid = UUIDs.timeBased();
        return new InboxMessage(to, uuid, msg, from);
    }
}
