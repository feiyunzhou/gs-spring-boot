package com.learner.messager;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Table("inbox")
@Data
public class Inbox {

  @PrimaryKey
  private InboxKey key;

  @Column("message_id")
  private UUID messageId;

  @Column("is_sent")
  private Boolean isSent;

  @Column("from_user")
  private String fromUser;

  public Inbox(final InboxKey key, final UUID messageId, final Boolean isSent, final String fromUser) {
    this.key = key;
    this.messageId = messageId;
    this.isSent = isSent;
    this.fromUser = fromUser;
  }

  public static Inbox createInboxMessage(final UUID messageId, final Boolean isSent, final String toUser, final String fromUser) {
      InboxKey inboxKey = InboxKey.createKey(toUser);
      Inbox inbox = new Inbox(inboxKey, messageId, isSent, fromUser);

      return inbox;
  }
  // getters and setters
}