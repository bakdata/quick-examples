package com.bakdata.quick.profilestore;

import com.bakdata.fluent_kafka_streams_tests.junit5.TestTopologyExtension;
import com.bakdata.quick.profilestore.utils.InstantSerde;
import io.d9p.demo.avro.ListeningEvent;
import java.time.Instant;
import java.util.List;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class UserFirstListenActivityTest {

    private static final String LISTENINGEVENTS = "le-test";
    private static final String FIRSTEVENTS = "first-topic";
    private final UserListenActivity app = createApp();

    @RegisterExtension
    final TestTopologyExtension<Long, ListeningEvent> testTopology =
            new TestTopologyExtension<>(props -> this.app.createTopology(), this.app.getKafkaProperties());

    @Test
    void shouldAggregateFirstForFirst() {
        this.testTopology.input(LISTENINGEVENTS)
                .add(1L, makeUserListenAtTime(1L, 2L))       // 1: last
                .add(1L, makeUserListenAtTime(1L, 0L))       // 1: first
                .add(2L, makeUserListenAtTime(1L, 100000L))  // 2: first/last
                .add(100L, makeUserListenAtTime(100L, 333L)) // 100: first
                .add(100L, makeUserListenAtTime(100L, 444L)) // 100: last
                .add(2L, makeUserListenAtTime(2L, 100000L))
                .add(1L, makeUserListenAtTime(1L, 0L));

        this.testTopology.streamOutput(FIRSTEVENTS)
                .withSerde(Serdes.Long(), new InstantSerde())
                .expectNextRecord().hasKey(1L).hasValue(Instant.ofEpochMilli(2))
                .expectNextRecord().hasKey(1L).hasValue(Instant.ofEpochMilli(0))
                .expectNextRecord().hasKey(2L).hasValue(Instant.ofEpochMilli(100000))
                .expectNextRecord().hasKey(100L).hasValue(Instant.ofEpochMilli(333))
                .expectNextRecord().hasKey(100L).hasValue(Instant.ofEpochMilli(333))
                .expectNextRecord().hasKey(2L).hasValue(Instant.ofEpochMilli(100000))
                .expectNextRecord().hasKey(1L).hasValue(Instant.ofEpochMilli(0))
                .expectNoMoreRecord();
    }

    private static UserListenActivity createApp() {
        final UserListenActivity app = new UserListenActivity();
        app.setInputTopics(List.of(LISTENINGEVENTS));
        app.setOutputTopic(FIRSTEVENTS);
        app.setKind(UserListenActivity.Kind.FIRST);
        return app;
    }

    static ListeningEvent makeUserListenAtTime(final Long userId, final Long timestampMillis) {
        return ListeningEvent.newBuilder().setUserId(userId).setArtistId(1L).setAlbumId(2L).setTrackId(3L)
                .setTimestamp(Instant.ofEpochMilli(timestampMillis)).build();
    }

}
