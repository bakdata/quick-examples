package com.bakdata.profilestore.recommender;

import com.bakdata.kafka.KafkaStreamsApplication;
import com.bakdata.profilestore.common.FieldType;
import com.bakdata.profilestore.recommender.avro.AdjacencyList;
import com.bakdata.profilestore.recommender.graph.BipartiteGraph;
import com.bakdata.profilestore.recommender.graph.KeyValueGraph;
import com.bakdata.profilestore.recommender.rest.RestService;
import io.d9p.demo.avro.Item;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.Stores;
import picocli.CommandLine;

@Slf4j
@Setter
public class RecommenderMain extends KafkaStreamsApplication {
    public static final String LEFT_INDEX_NAME = "recommender-left-index";
    public static final String RIGHT_INDEX_NAME = "recommender-right-index";
    public static final Map<FieldType, String> storeNames = getStoreNames();


    @CommandLine.Option(names = "--host", defaultValue = "localhost", description = "address of host machine")
    private String host;

    @CommandLine.Option(names = "--port", defaultValue = "8080", description = "port of REST service")
    private int port;

    @CommandLine.Option(names = "--id-resolution", defaultValue = "false", arity = "1",
        description = "Perform id resolution, producing named recommendations")
    private boolean idResolution;

    public static void main(final String[] args) {
        startApplication(new RecommenderMain(), addFallbackHostFromEnv(args));
    }

    public static String[] addFallbackHostFromEnv(final String[] args) {
        // host and port can also be set from env (will override default)
        final List<String> argList = new ArrayList<>(List.of(args));
        final String host = System.getenv("POD_IP");
        final String port = System.getenv("CONTAINER_PORT");
        if (!argList.contains("--host") & host != null) {
            argList.addAll(List.of("--host", host));
        }
        if (!argList.contains("--port") & port != null) {
            argList.addAll(List.of("--port", port));
        }
        return argList.toArray(new String[0]);
    }

    @Override
    public String getUniqueAppId() {
        return String.format("recommender-%s-%s", this.host, this.port);
    }

    @Override
    public void run() {
        super.run();
        if (!this.cleanUp) {
            try {
                this.runRestservice();
            } catch (final Exception e) {
                log.error("Could not start rest service: {}", e.getMessage());
            }
        }
    }

    private void runRestservice() throws Exception {
        final KafkaStreams streams = this.getStreams();
        waitForKafkaStreams(streams);

        final Map<FieldType, BipartiteGraph> graph = getGraph(streams);

        final RestService restService =
                new RestService(new HostInfo(this.host, this.port), graph, streams, storeNames, this.idResolution);
        restService.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                streams.close();
                restService.stop();
            } catch (final Exception e) {
                log.warn("Error in shutdown", e);
            }
        }));

    }

    @Override
    public Properties createKafkaProperties() {
        final Properties props = super.createKafkaProperties();
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, String.format("%s:%s", this.host, this.port));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Long().getClass().getName());
        return props;
    }

    /**
     * Get the bipartite graph of the application
     *
     * @param streams the KafkaStreams instance
     * @return BipartiteGraph instance that represents the left and right index
     */
    private static Map<FieldType, BipartiteGraph> getGraph(final KafkaStreams streams) {
        final Map<FieldType, BipartiteGraph> graphs = new EnumMap<>(FieldType.class);
        for (final FieldType type : FieldType.values()) {
            graphs.put(type, new KeyValueGraph(
                    streams.store(StoreQueryParameters.fromNameAndType(RecommenderProcessor.getLeftIndexName(type),
                            QueryableStoreTypes.keyValueStore())),
                    streams.store(StoreQueryParameters.fromNameAndType(RecommenderProcessor.getRightIndexName(type),
                            QueryableStoreTypes.keyValueStore()))
            ));
        }
        return graphs;
    }

    public <T extends SpecificRecord> SpecificAvroSerde<T> getConfiguredSerde(final Class<T> avroType,
            final boolean isSerdeForRecordKeys) {
        final Map<String, String> serdeConfig = Collections.singletonMap(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.getSchemaRegistryUrl());
        final SpecificAvroSerde<T> serDe = new SpecificAvroSerde<>();
        serDe.configure(serdeConfig, isSerdeForRecordKeys);
        return serDe;
    }

    @Override
    public void buildTopology(final StreamsBuilder builder) {

        final SpecificAvroSerde<AdjacencyList> adjacencyListSerde = this.getConfiguredSerde(AdjacencyList.class, false);
        final SpecificAvroSerde<Item> namedRecordSerde = this.getConfiguredSerde(Item.class, false);

        if (this.idResolution) {
            RecommenderMain.addGlobalNameStore(builder, this.getInputTopic("album"), storeNames.get(FieldType.ALBUM),
                    namedRecordSerde);
            RecommenderMain.addGlobalNameStore(builder, this.getInputTopic("artist"), storeNames.get(FieldType.ARTIST),
                    namedRecordSerde);
            RecommenderMain.addGlobalNameStore(builder, this.getInputTopic("track"), storeNames.get(FieldType.TRACK),
                    namedRecordSerde);
        }
        Topology topology = builder.build();

        topology
                .addSource("interaction-source", this.getInputTopic())
                .addProcessor("interaction-processor", RecommenderProcessor::new, "interaction-source");

        for (final FieldType type : FieldType.values()) {
            topology = RecommenderMain.addStateStores(topology, type, adjacencyListSerde);
        }
    }

    /**
     * Adds a new state store for every RecommendationType
     *
     * @param topology base topology
     * @param type type for which the processor should be added
     * @param adjacencyListSerde serde
     * @return updated Topology
     */
    private static Topology addStateStores(final Topology topology, final FieldType type,
            final SpecificAvroSerde<AdjacencyList> adjacencyListSerde) {
        return topology
                .addStateStore(Stores.keyValueStoreBuilder(
                        Stores.inMemoryKeyValueStore(RecommenderProcessor.getLeftIndexName(type)),
                        Serdes.Long(), adjacencyListSerde), "interaction-processor")
                .addStateStore(Stores.keyValueStoreBuilder(
                        Stores.inMemoryKeyValueStore(RecommenderProcessor.getRightIndexName(type)),
                        Serdes.Long(), adjacencyListSerde), "interaction-processor");
    }

    private static void addGlobalNameStore(final StreamsBuilder builder, final String topicName, final String storeName,
            final SpecificAvroSerde<Item> recordSerde) {
        builder.globalTable(topicName,
                Materialized.<Long, Item, KeyValueStore<Bytes, byte[]>>as(storeName)
                        .withKeySerde(Serdes.Long())
                        .withValueSerde(recordSerde));
    }

    /**
     * Wait for the application to change status to running
     *
     * @param streams the KafkaStreams instance
     */
    private static void waitForKafkaStreams(final KafkaStreams streams) throws Exception {
        while (true) {
            try {
                getGraph(streams);
                return;
            } catch (final InvalidStateStoreException ignored) {
                // store not yet ready for querying
                log.debug("Store not available");
                Thread.sleep(1000);
            }
        }
    }

    private static Map<FieldType, String> getStoreNames() {
        return Stream.of(FieldType.values()).collect(Collectors.toMap(
                type -> type,
                type -> type.toString().toLowerCase() + "-store"));
    }

}
