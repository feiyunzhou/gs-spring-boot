package com.learner;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

@Table("user")
@Data
@Log4j2
public class User {
  @PrimaryKeyColumn(name = "username", type = PARTITIONED)
  private String userName;

  @Column
  private String password;

  public User(final String userName, String password) {
    this.userName = userName;
    this.password = password;
  }
  public User() {}
  // getters and setters
}