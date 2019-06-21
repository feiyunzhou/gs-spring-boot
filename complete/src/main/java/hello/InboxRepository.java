package hello;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface InboxRepository extends CassandraRepository<Inbox, InboxKey> {
    List<Inbox> findInboxesByKey_UsernameAndKey_TimeGreaterThan(final String username, final UUID time);

    @Query("select * from inbox where username = ?0 and time > ?1")
    List<Inbox> getInboxMessages(String userName, UUID time);

    List<Inbox> getInboxesByKey_UsernameAndKey_TimeGreaterThan(String userName, UUID time);
}