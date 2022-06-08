import com.bakdata.fluent_kafka_streams_tests.junit5.TestTopologyExtension;
import com.bakdata.profilestore.common.FieldType;
import io.d9p.demo.avro.Item;
import io.d9p.demo.avro.ListeningEvent;
import io.d9p.demo.avro.NamedChartRecord;
import io.d9p.demo.avro.NamedCharts;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class UserListenChartsTest {

    private static final String LISTENINGEVENTS = "le-test";
    private static final String USERCHARTS = "top3";
    private static final String ARTIST_IDS = "artist-ids";
    private final UserListenCharts app = createApp();

    static ListeningEvent makeUserListensArtist(final Long userId, final Long artistId) {
        return ListeningEvent.newBuilder().setUserId(userId).setArtistId(artistId).setAlbumId(1L).setTrackId(2L)
                .setTimestamp(Instant.now()).build();
    }

    static NamedCharts makeUserCharts(final Map<Long, Long> frequencies) {
        final List<NamedChartRecord> charts = frequencies.entrySet()
                .stream()
                .map(entry -> new NamedChartRecord(entry.getKey(), String.format("artist%d", entry.getKey()),
                        entry.getValue()))
                .sorted(Comparator.comparingLong(NamedChartRecord::getCountPlays).reversed()
                        .thenComparing(NamedChartRecord::getName))
                .collect(Collectors.toList());
        final NamedCharts userCharts = new NamedCharts();
        userCharts.setTopK(charts);
        return userCharts;
    }

    @RegisterExtension
    final TestTopologyExtension<Long, SpecificRecord> testTopology =
            new TestTopologyExtension<>(props -> this.app.createTopology(), this.app.getKafkaProperties());

    @BeforeEach
    void fillJoinTable() {
        this.testTopology.input(ARTIST_IDS)
                .add(1L, new Item(1L, "artist1"))
                .add(2L, new Item(2L, "artist2"))
                .add(3L, new Item(3L, "artist3"));
    }

    @Test
    void shouldCollectTopK() {
        this.testTopology.input(LISTENINGEVENTS)
                .add(1L, makeUserListensArtist(1L, 1L))
                .add(1L, makeUserListensArtist(1L, 2L))
                .add(2L, makeUserListensArtist(2L, 1L))
                .add(2L, makeUserListensArtist(2L, 1L))
                .add(1L, makeUserListensArtist(1L, 1L))
                .add(1L, makeUserListensArtist(1L, 3L))
                .add(2L, makeUserListensArtist(2L, 2L))
                .add(1L, makeUserListensArtist(1L, 2L))
                .add(1L, makeUserListensArtist(1L, 2L)); // user 1: 2x1, 3x2, 1x3, user 2: 2x1, 1x2

        this.testTopology.streamOutput(USERCHARTS)
                .expectNextRecord().hasKey(1L).hasValue(makeUserCharts(Map.of(1L, 1L)))
                .expectNextRecord().hasKey(1L).hasValue(makeUserCharts(Map.of(1L, 1L, 2L, 1L)))
                .expectNextRecord().hasKey(2L).hasValue(makeUserCharts(Map.of(1L, 1L)))
                .expectNextRecord().hasKey(2L).hasValue(makeUserCharts(Map.of(1L, 2L)))
                .expectNextRecord().hasKey(1L).hasValue(makeUserCharts(Map.of(1L, 2L, 2L, 1L)))
                .expectNextRecord().hasKey(1L).hasValue(makeUserCharts(Map.of(1L, 2L, 2L, 1L, 3L, 1L)))
                .expectNextRecord().hasKey(2L).hasValue(makeUserCharts(Map.of(1L, 2L, 2L, 1L)))
                .expectNextRecord().hasKey(1L).hasValue(makeUserCharts(Map.of(1L, 2L, 2L, 2L, 3L, 1L)))
                .expectNextRecord().hasKey(1L).hasValue(makeUserCharts(Map.of(1L, 2L, 2L, 3L, 3L, 1L)))
                .expectNoMoreRecord();
    }

    @Test
    void shouldNotCollectMoreThanK() {
        for (long i = 0L; i < this.app.getK() + 10; i++) {
            this.testTopology.input(LISTENINGEVENTS).add(1L, makeUserListensArtist(1L, i));
        }
        for (final ProducerRecord<Long, SpecificRecord> producerRecord : this.testTopology.streamOutput(USERCHARTS)) {
            assert ((NamedCharts) producerRecord.value()).getTopK().size() <= this.app.getK();
        }

    }

    private static UserListenCharts createApp() {
        final UserListenCharts app = new UserListenCharts();
        app.setInputTopics(List.of(LISTENINGEVENTS));
        app.setExtraInputTopics(Map.of("artist", ARTIST_IDS, "album", "-", "track", "-"));
        app.setOutputTopic(USERCHARTS);
        app.setFieldType(FieldType.ARTIST);
        app.setK(3);
        app.setSchemaRegistryUrl("mock://example:9999");
        app.setIdResolution(true);
        return app;
    }
}
