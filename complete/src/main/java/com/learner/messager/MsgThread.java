package com.learner.messager;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;
import java.util.UUID;

import static org.springframework.data.cassandra.core.cql.Ordering.DESCENDING;
import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

@Table("msg_thread")
@Data
@ToString
public class MsgThread {
    @PrimaryKeyColumn(name = "thread_id", type = PARTITIONED)
    private UUID threadId;
    @PrimaryKeyColumn(name = "username", ordinal = 1, ordering = DESCENDING)
    private String userName;

    @Column("thread_type")
    private int threadType;

    @Column("create_time")
    private Date createTime;
}
