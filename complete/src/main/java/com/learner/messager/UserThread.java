package com.learner.messager;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

/**
 * VO 类用于保存基于用户名字的索引，也就是可以根据用户查询该用户参与的thread
 *
 */
@Table("user_thread")
@Data
@ToString
public class UserThread {
    @PrimaryKeyColumn(name = "username", type = PARTITIONED)
    private String userName;
    @Column("thread_id")
    private UUID threadId;
}
