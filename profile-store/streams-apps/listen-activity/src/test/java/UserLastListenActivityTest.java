import com.bakdata.fluent_kafka_streams_tests.junit5.TestTopologyExtension;
import com.bakdata.profilestore.common.utils.InstantSerde;
import io.d9p.demo.avro.ListeningEvent;
import java.time.Instant;
import java.util.List;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class UserLastListenActivityTest {

    private static final String LISTENINGEVENTS = "le-test";
    private static final String LASTEVENTS = "last-topic";
    private final UserListenActivity app = createApp();


    @RegisterExtension
    final TestTopologyExtension<Long, ListeningEvent> testTopology =
            new TestTopologyExtension<>(props -> this.app.createTopology(), this.app.getKafkaProperties());

    @Test
    void shouldAggregateLastForLast() {
        final ListeningEvent.Builder builder = ListeningEvent.newBuilder()
                .setArtistId(1L)
                .setAlbumId(2L)
                .setTrackId(3L);

        this.testTopology.input(LISTENINGEVENTS)
                .add(1L, makeUserListenAtTime(1L, 2L))       // 1: last
                .add(1L, makeUserListenAtTime(1L, 0L))       // 1: first
                .add(2L, makeUserListenAtTime(1L, 100000L))  // 2: first/last
                .add(100L, makeUserListenAtTime(100L, 333L)) // 100: first
                .add(100L, makeUserListenAtTime(100L, 444L)) // 100: last
                .add(2L, makeUserListenAtTime(2L, 100000L))
                .add(1L, makeUserListenAtTime(1L, 0L));

        this.testTopology.streamOutput(LASTEVENTS)
                .withSerde(Serdes.Long(), new InstantSerde())
                .expectNextRecord().hasKey(1L).hasValue(Instant.ofEpochMilli(2))
                .expectNextRecord().hasKey(1L).hasValue(Instant.ofEpochMilli(2))
                .expectNextRecord().hasKey(2L).hasValue(Instant.ofEpochMilli(100000))
                .expectNextRecord().hasKey(100L).hasValue(Instant.ofEpochMilli(333))
                .expectNextRecord().hasKey(100L).hasValue(Instant.ofEpochMilli(444))
                .expectNextRecord().hasKey(2L).hasValue(Instant.ofEpochMilli(100000))
                .expectNextRecord().hasKey(1L).hasValue(Instant.ofEpochMilli(2))
                .expectNoMoreRecord();
    }

    private static UserListenActivity createApp() {
        final UserListenActivity app = new UserListenActivity();
        app.setInputTopics(List.of(LISTENINGEVENTS));
        app.setOutputTopic(LASTEVENTS);
        app.setKind(UserListenActivity.Kind.LAST);
        return app;
    }

    static ListeningEvent makeUserListenAtTime(final Long userId, final Long timestampMillis) {
        return ListeningEvent.newBuilder().setUserId(userId).setArtistId(1L).setAlbumId(2L).setTrackId(3L)
                .setTimestamp(Instant.ofEpochMilli(timestampMillis)).build();
    }
}
