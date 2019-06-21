package hello;

import com.datastax.driver.core.utils.UUIDs;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.util.UUID;

import static org.springframework.data.cassandra.core.cql.Ordering.DESCENDING;
import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.PARTITIONED;

@PrimaryKeyClass
@Data
public class InboxKey {
    @PrimaryKeyColumn(name = "username", type = PARTITIONED)
    private String username;

    @PrimaryKeyColumn(name = "time", ordinal = 1, ordering = DESCENDING)
    private UUID time;

    public static InboxKey createKey(String username) {
        InboxKey key = new InboxKey();
        key.setUsername(username);
        key.setTime(UUIDs.timeBased());
        return key;
    }
}
