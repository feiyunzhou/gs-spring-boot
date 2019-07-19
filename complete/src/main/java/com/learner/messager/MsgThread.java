package com.learner.messager;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

/**
 * 消息thread表VO，创建者owner, threadType，确定是单聊(0)还是群聊(1)，participants:是参与聊天的userNames
 */
@Table("msg_thread")
@Data
@ToString
public class MsgThread {
    @PrimaryKeyColumn(name = "thread_id", type = PARTITIONED)
    private UUID threadId;
    @Column("owner")
    private String owner;

    @Column("thread_type")
    private int threadType;
    @Column("participants")
    private Set<String> participants;
    @Column("modify_time")
    private Date modifyTime;
    @Column("create_time")
    private Date createTime;
    @Transient
    private String name;
}
