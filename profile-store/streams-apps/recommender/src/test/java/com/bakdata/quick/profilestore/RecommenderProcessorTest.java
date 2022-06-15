package com.bakdata.quick.profilestore;

import com.bakdata.fluent_kafka_streams_tests.TestInput;
import com.bakdata.fluent_kafka_streams_tests.junit5.TestTopologyExtension;
import com.bakdata.quick.profilestore.graph.BipartiteGraph;
import com.bakdata.quick.profilestore.graph.KeyValueGraph;
import io.d9p.demo.avro.Item;
import io.d9p.demo.avro.ListeningEvent;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serdes.LongSerde;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RecommenderProcessorTest {
    static final String LISTENING_EVENT_INPUT = "listening-events";
    static final String ARTIST_INPUT = "artist-input";
    static final String ALBUM_INPUT = "album-input";
    static final String TRACK_INPUT = "track-input";
    private final RecommenderMain app = createApp();

    private static RecommenderMain createApp() {
        final RecommenderMain recommender = new RecommenderMain();
        recommender.setInputTopics(List.of(LISTENING_EVENT_INPUT));
        recommender.setExtraInputTopics(
                Map.of("artist", ARTIST_INPUT, "album", ALBUM_INPUT, "track", TRACK_INPUT));
        recommender.setSchemaRegistryUrl("mock://something");
        recommender.setIdResolution(true);
        return recommender;
    }

    @RegisterExtension
    final TestTopologyExtension<String, ListeningEvent> testTopology =
            new TestTopologyExtension<>(props -> this.app.createTopology(), RecommenderProcessorTest.getProperties());

    @BeforeEach
    void fillTables() {
        for (final String input : List.of(ARTIST_INPUT, ALBUM_INPUT, TRACK_INPUT)) {
            final String namePrefix = input.split("-")[0].toUpperCase();
            final TestInput<Long, Item> globalInput =
                    this.testTopology.input(input)
                            .withSerde(Serdes.Long(), this.app.getConfiguredSerde(Item.class, false));
            LongStream.range(0, 50).forEach(
                    i -> globalInput.add(i, Item.newBuilder().setId(i).setName(namePrefix + "_" + i).build()));
        }
    }


    @Test
    void testAlbumSingleInput() {
        this.testTopology.input(LISTENING_EVENT_INPUT)
                .add(new ListeningEvent(1L, 2L, 3L, 4L, Instant.now()));
        final EnumMap<FieldType, BipartiteGraph> graphMap = this.getGraphMap();

        Assertions.assertEquals(Collections.singletonList(3L),
                graphMap.get(FieldType.ALBUM).getLeftNodeNeighbors(1));
        Assertions.assertEquals(Collections.singletonList(1L),
                graphMap.get(FieldType.ALBUM).getRightNodeNeighbors(3));
    }

    @Test
    void testArtistSingleInput() {
        this.testTopology.input(LISTENING_EVENT_INPUT)
                .add(new ListeningEvent(1L, 2L, 3L, 4L, Instant.now()));
        final EnumMap<FieldType, BipartiteGraph> graphMap = this.getGraphMap();

        Assertions.assertEquals(Collections.singletonList(2L),
                graphMap.get(FieldType.ARTIST).getLeftNodeNeighbors(1));
        Assertions.assertEquals(Collections.singletonList(1L),
                graphMap.get(FieldType.ARTIST).getRightNodeNeighbors(2));
    }

    @Test
    void testTrackSingleInput() {
        this.testTopology.input(LISTENING_EVENT_INPUT)
                .add(new ListeningEvent(1L, 2L, 3L, 4L, Instant.now()));
        final EnumMap<FieldType, BipartiteGraph> graphMap = this.getGraphMap();

        Assertions.assertEquals(Collections.singletonList(4L),
                graphMap.get(FieldType.TRACK).getLeftNodeNeighbors(1));
        Assertions.assertEquals(Collections.singletonList(1L),
                graphMap.get(FieldType.TRACK).getRightNodeNeighbors(4));
    }

    @Test
    void testMultipleInputs() {
        final long[] users = {1, 5, 1, 6, 1};
        final long[] artists = {2, 3, 4, 5, 2};
        final long[] album = {3, 3, 4, 5, 6};
        final long[] track = {4, 8, 2, 8, 7};

        final TestInput<String, ListeningEvent> testInput = this.testTopology.input(LISTENING_EVENT_INPUT);

        for (int i = 0; i < users.length; i++) {
            testInput.add(new ListeningEvent(users[i], artists[i], album[i], track[i], Instant.now()));
        }

        final EnumMap<FieldType, BipartiteGraph> graphMap = this.getGraphMap();

        Assertions.assertEquals(Arrays.asList(3L, 4L, 6L),
                graphMap.get(FieldType.ALBUM).getLeftNodeNeighbors(1));
        Assertions.assertEquals(Collections.singletonList(1L),
                graphMap.get(FieldType.ALBUM).getRightNodeNeighbors(4));

        Assertions.assertEquals(Arrays.asList(2L, 4L, 2L),
                graphMap.get(FieldType.ARTIST).getLeftNodeNeighbors(1));
        Assertions.assertEquals(Arrays.asList(1L, 1L),
                graphMap.get(FieldType.ARTIST).getRightNodeNeighbors(2));

        Assertions.assertEquals(Arrays.asList(4L, 2L, 7L),
                graphMap.get(FieldType.TRACK).getLeftNodeNeighbors(1));
        Assertions.assertEquals(Arrays.asList(5L, 6L),
                graphMap.get(FieldType.TRACK).getRightNodeNeighbors(8));

    }

    @Test
    void testTrackNameResolving() {
        final ReadOnlyKeyValueStore<Long, Item> nameTable =
                this.testTopology.getTestDriver().getKeyValueStore(RecommenderMain.storeNames.get(FieldType.TRACK));

        final List<Long> trackIds = List.of(4L, 8L, 2L, 7L);
        final List<Item> trackNames = trackIds.stream()
                .map(nameTable::get)
                .collect(Collectors.toList());

        final List<Item> expectedTrackNames = List.of(
                Item.newBuilder().setId(4L).setName("TRACK_4").build(),
                Item.newBuilder().setId(8L).setName("TRACK_8").build(),
                Item.newBuilder().setId(2L).setName("TRACK_2").build(),
                Item.newBuilder().setId(7L).setName("TRACK_7").build()
        );

        MatcherAssert.assertThat(trackNames,
                IsIterableContainingInAnyOrder.containsInAnyOrder(expectedTrackNames.toArray()));
    }

    @Test
    void testAlbumNameResolving() {
        final ReadOnlyKeyValueStore<Long, Item> nameTable =
                this.testTopology.getTestDriver().getKeyValueStore(RecommenderMain.storeNames.get(FieldType.ALBUM));

        final List<Long> trackIds = List.of(4L, 8L, 2L, 7L);
        final List<Item> trackNames = trackIds.stream()
                .map(nameTable::get)
                .collect(Collectors.toList());

        final List<Item> expectedTrackNames = List.of(
                Item.newBuilder().setId(4L).setName("ALBUM_4").build(),
                Item.newBuilder().setId(8L).setName("ALBUM_8").build(),
                Item.newBuilder().setId(2L).setName("ALBUM_2").build(),
                Item.newBuilder().setId(7L).setName("ALBUM_7").build()
        );

        MatcherAssert.assertThat(trackNames,
                IsIterableContainingInAnyOrder.containsInAnyOrder(expectedTrackNames.toArray()));
    }

    @Test
    void testArtistNameResolving() {
        final ReadOnlyKeyValueStore<Long, Item> nameTable =
                this.testTopology.getTestDriver().getKeyValueStore(RecommenderMain.storeNames.get(FieldType.ARTIST));

        final List<Long> trackIds = List.of(4L, 8L, 2L, 7L);
        final List<Item> trackNames = trackIds.stream()
                .map(nameTable::get)
                .collect(Collectors.toList());

        final List<Item> expectedTrackNames = List.of(
                Item.newBuilder().setId(4L).setName("ARTIST_4").build(),
                Item.newBuilder().setId(8L).setName("ARTIST_8").build(),
                Item.newBuilder().setId(2L).setName("ARTIST_2").build(),
                Item.newBuilder().setId(7L).setName("ARTIST_7").build()
        );

        MatcherAssert.assertThat(trackNames,
                IsIterableContainingInAnyOrder.containsInAnyOrder(expectedTrackNames.toArray()));
    }

    EnumMap<FieldType, BipartiteGraph> getGraphMap() {
        final EnumMap<FieldType, BipartiteGraph> graphs =
                new EnumMap<>(FieldType.class);
        for (final FieldType type : FieldType.values()) {
            final BipartiteGraph graph = new KeyValueGraph(
                    this.testTopology.getTestDriver().getKeyValueStore(RecommenderProcessor.getLeftIndexName(type)),
                    this.testTopology.getTestDriver().getKeyValueStore(RecommenderProcessor.getRightIndexName(type))
            );
            graphs.put(type, graph);
        }
        return graphs;
    }

    private static Properties getProperties() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "profile-topology-test");
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "dummy:1234");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, LongSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        return props;
    }
}
