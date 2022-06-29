package com.bakdata.quick.profilestore;

import com.bakdata.fluent_kafka_streams_tests.junit5.TestTopologyExtension;
import com.bakdata.quick.profilestore.UserListenCountApp;
import io.d9p.demo.avro.ListeningEvent;
import java.time.Instant;
import java.util.List;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class UserListenCountAppTest {

    private static final String LISTENINGEVENTS = "le-test";
    private static final String LISTENCOUNTS = "le-count-test";
    private final UserListenCountApp app = createApp();

    @RegisterExtension
    final TestTopologyExtension<Long, ListeningEvent> testTopology =
            new TestTopologyExtension<>(props -> this.app.createTopology(), this.app.getKafkaProperties());

    @Test
    void shouldCountPerUser() {
        final ListeningEvent.Builder builder = ListeningEvent.newBuilder()
                .setArtistId(1L)
                .setAlbumId(2L)
                .setTrackId(3L)
                .setTimestamp(Instant.now());
        this.testTopology.input(LISTENINGEVENTS)
                .add(1L, builder.setUserId(1L).build())
                .add(1L, builder.setUserId(1L).build())
                .add(2L, builder.setUserId(2L).build())
                .add(100L, builder.setUserId(100L).build())
                .add(1L, builder.setUserId(1L).build());

        this.testTopology.streamOutput(LISTENCOUNTS)
                .withSerde(Serdes.Long(), Serdes.Long())
                .expectNextRecord().hasKey(1L).hasValue(1L)
                .expectNextRecord().hasKey(1L).hasValue(2L)
                .expectNextRecord().hasKey(2L).hasValue(1L)
                .expectNextRecord().hasKey(100L).hasValue(1L)
                .expectNextRecord().hasKey(1L).hasValue(3L)
                .expectNoMoreRecord();

    }

    private static UserListenCountApp createApp() {
        final UserListenCountApp app = new UserListenCountApp();
        app.setInputTopics(List.of(LISTENINGEVENTS));
        app.setOutputTopic(LISTENCOUNTS);
        return app;
    }
}
