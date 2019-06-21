package hello;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("user")
@Setter
@Getter
public class User {

  @PrimaryKey
  private int id;

  @Column("user_name")
  private String userName;

  public User(final int key, final String userName) {
    this.id = key;
    this.userName = userName;
  }
  // getters and setters
}