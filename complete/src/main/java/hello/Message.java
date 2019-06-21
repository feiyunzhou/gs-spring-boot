package hello;

import com.datastax.driver.core.utils.UUIDs;
import lombok.Data;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Table("message")
@Data
public class Message {
    @PrimaryKey("message_id")
    private UUID messageId;

    @Column("username")
    private String userName;

    @Column("body")
    private String body;

    @Transient
    private String to;

    public static Message createMessage(String userName, String body) {
        Message msg = new Message();
        msg.setBody(body);
        msg.setUserName(userName);

        msg.setMessageId(UUIDs.random());
        return msg;
    }
}
