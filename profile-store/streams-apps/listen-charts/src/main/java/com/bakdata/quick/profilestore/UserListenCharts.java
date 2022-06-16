package com.bakdata.quick.profilestore;

import com.bakdata.kafka.KafkaStreamsApplication;
import com.bakdata.quick.profilestore.avro.CompositeKey;
import io.d9p.demo.avro.ChartRecord;
import io.d9p.demo.avro.Item;
import io.d9p.demo.avro.ListeningEvent;
import io.d9p.demo.avro.NamedChartRecord;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Repartitioned;
import picocli.CommandLine;

/**
 * Aggregates the top k artists / albums / tracks for each user
 */
@Getter
@Setter
public class UserListenCharts extends KafkaStreamsApplication {

    @CommandLine.Option(names = "-k", defaultValue = "10",
            description = "Keep track of top <k> most listened instances")
    private int K;

    @CommandLine.Option(names = "--field", required = true, description = "Make chart for ARTIST/ALBUM/TRACK")
    private FieldType fieldType;

    @CommandLine.Option(names = "--id-resolution", defaultValue = "false", arity = "1",
        description = "Perform id resolution, producing named charts")
    private boolean idResolution;

    public static void main(final String[] args) {
        startApplication(new UserListenCharts(), args);
    }

    @Override
    public void buildTopology(final StreamsBuilder builder) {
        if (this.idResolution) {
            final GlobalKTable<Long, Item> joinTable = this.createJoinTable(builder);
            final KStream<Long, NamedChartRecord> namedChartRecordKStream =
                    this.createNamedRecordsPerUser(builder, joinTable);
            this.createTopKStream(namedChartRecordKStream, new NamedChartAggregator(this.K));
        } else {
            final KStream<Long, ChartRecord> chartRecordKStream = this.createChartRecordsPerUser(builder);
            this.createTopKStream(chartRecordKStream, new AnonymousChartAggregator(this.K));
        }
    }

    private <ChartT, ChartRecordT> void createTopKStream(final KStream<Long, ChartRecordT> chartRecordKStream,
            final ChartAggregationWrapper<ChartT, ChartRecordT> chartAggregator) {
        chartRecordKStream
                .groupByKey()
                .aggregate(chartAggregator::initialize, chartAggregator::update)
                .toStream()
                .to(this.getOutputTopic());
    }


    @Override
    public String getUniqueAppId() {
        final String appId = "user-listen-charts-" + this.fieldKey();
        return this.idResolution ? appId + "-named" : appId;
    }

    private String fieldKey() {
        return this.fieldType.name().toLowerCase();
    }

    @Override
    public Properties createKafkaProperties() {
        final Properties kafkaConfig = super.createKafkaProperties();
        kafkaConfig.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Long().getClass().getName());
        return kafkaConfig;
    }

    private GlobalKTable<Long, Item> createJoinTable(final StreamsBuilder builder) {
        return builder.globalTable(this.getInputTopic(this.fieldKey()), Materialized.as(this.fieldKey() + "_table"));
    }

    private KStream<Long, NamedChartRecord> createNamedRecordsPerUser(final StreamsBuilder builder,
            final GlobalKTable<Long, Item> joinTable) {
        final KStream<Long, ChartRecord> chartRecordsPerUser = this.createChartRecordsPerUser(builder);
        return chartRecordsPerUser
                .join(joinTable,
                        (userId, chartRecord) -> chartRecord.getId(),
                        (chartRecord, fieldRecord) -> new NamedChartRecord(chartRecord.getId(),
                                fieldRecord.getName(),
                                chartRecord.getCountPlays()));
    }

    private KStream<Long, ChartRecord> createChartRecordsPerUser(final StreamsBuilder builder) {

        final Map<String, String> serdeConfig =
                Collections.singletonMap(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                        this.getSchemaRegistryUrl());
        final SpecificAvroSerde<CompositeKey> compositeKeySerDe = new SpecificAvroSerde<>();
        compositeKeySerDe.configure(serdeConfig, true);

        final KStream<Long, ListeningEvent> inputStream = builder.stream(this.getInputTopic());

        // get counts for tuple (userId, field) as KTable
        final KTable<CompositeKey, Long> fieldCountsPerUser = inputStream
                .map((key, event) -> KeyValue.pair(key, this.getField(event)))
                .groupBy(CompositeKey::new, Grouped.with(compositeKeySerDe, Serdes.Long()))
                .count();

        // (userId, field) -> count TO userId -> (field, count)
        return fieldCountsPerUser
                .toStream()
                .map((key, count) ->
                        KeyValue.pair(
                                key.getPrimaryKey(),
                                new ChartRecord(key.getSecondaryKey(), count)
                        ))
                .repartition(Repartitioned.as("counts"));
    }

    private long getField(final ListeningEvent listeningEvent) {
        switch (this.fieldType) {
            case ARTIST:
                return listeningEvent.getArtistId();
            case ALBUM:
                return listeningEvent.getAlbumId();
            case TRACK:
                return listeningEvent.getTrackId();
        }
        // this should be impossible
        throw new AssertionError("Unknown fieldType set");
    }

}
