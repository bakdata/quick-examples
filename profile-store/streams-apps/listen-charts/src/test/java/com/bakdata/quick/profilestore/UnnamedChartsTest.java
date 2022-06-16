package com.bakdata.quick.profilestore;

import com.bakdata.fluent_kafka_streams_tests.junit5.TestTopologyExtension;
import io.d9p.demo.avro.ChartRecord;
import io.d9p.demo.avro.Charts;
import io.d9p.demo.avro.ListeningEvent;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class UnnamedChartsTest {
    private static final String LISTENINGEVENTS = "le-test";
    private static final String USERCHARTS = "top3";
    private static final String ARTIST_IDS = "artist-ids";
    final UserListenCharts app = createApp();

    static ListeningEvent makeUserListensArtist(final Long userId, final Long artistId) {
        return ListeningEvent.newBuilder().setUserId(userId).setArtistId(artistId).setAlbumId(1L).setTrackId(2L)
                .setTimestamp(Instant.now()).build();
    }

    static Charts makeUserCharts(final Map<Long, Long> frequencies) {
        final List<ChartRecord> charts = frequencies.entrySet()
                .stream()
                .map(entry -> new ChartRecord(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(ChartRecord::getCountPlays).reversed()
                        .thenComparing(ChartRecord::getId))
                .collect(Collectors.toList());
        final Charts userCharts = new Charts();
        userCharts.setTopK(charts);
        return userCharts;
    }

    @RegisterExtension
    final TestTopologyExtension<Long, SpecificRecord> testTopology =
            new TestTopologyExtension<>(props -> this.app.createTopology(), this.app.getKafkaProperties());

    @Test
    void shouldProduceAnonymousCharts() {
        this.testTopology.input(LISTENINGEVENTS)
                .add(1L, makeUserListensArtist(1L, 1L))
                .add(1L, makeUserListensArtist(1L, 2L));

        this.testTopology.streamOutput(USERCHARTS)
                .expectNextRecord().hasValue(makeUserCharts(Map.of(1L, 1L)))
                .expectNextRecord().hasValue(makeUserCharts(Map.of(1L, 1L, 2L, 1L)))
                .expectNoMoreRecord();
    }

    private static UserListenCharts createApp() {
        final UserListenCharts app = new UserListenCharts();
        app.setInputTopics(List.of(LISTENINGEVENTS));
        app.setExtraInputTopics(Map.of("artist", ARTIST_IDS, "album", "-", "track", "-"));
        app.setOutputTopic(USERCHARTS);
        app.setFieldType(FieldType.ARTIST);
        app.setK(3);
        app.setSchemaRegistryUrl("mock://example:9999");
        app.setIdResolution(false);
        return app;
    }
}
