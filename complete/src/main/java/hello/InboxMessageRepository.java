package hello;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InboxMessageRepository extends CassandraRepository<InboxMessage, String> {
    List<InboxMessage> getInboxMessagesByToAndTimeGreaterThan(String to, UUID time);
}