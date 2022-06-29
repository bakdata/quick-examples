package com.bakdata.demo;

import com.bakdata.kafka.KafkaStreamsApplication;
import com.bakdata.quick.avro.Status;
import com.bakdata.quick.avro.Trip;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

public class TripAggregationApp extends KafkaStreamsApplication {

    public static final String URL_CONFIG = AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

    public static void main(final String[] args) {
        KafkaStreamsApplication.startApplication(new TripAggregationApp(), args);
    }

    private static Trip aggregateTrip(final String key, final Status value, final Trip aggregate) {
        List<Status> route = aggregate.getRoute();
        // first time we see this trip id
        if (route == null) {
            aggregate.setId(value.getTripId());
            aggregate.setVehicleId(value.getVehicleId());
            route = new ArrayList<>();
        }

        // endless mode; remove all elements if we see the first Id again
        if (!route.isEmpty() && route.get(0).getStatusId().equals(value.getStatusId())) {
            route.clear();
        }

        route.add(value);
        // they might not be sorted, we need to make sure because we rely on it downstream
        route.sort(Comparator.comparing(Status::getTimestamp));
        aggregate.setRoute(route);
        return aggregate;
    }

    @Override
    public void buildTopology(final StreamsBuilder builder) {
        final Serde<String> stringSerde = Serdes.String();
        final Serde<Status> statusSerde = new SpecificAvroSerde<>();
        final Serde<Trip> tripSerde = new SpecificAvroSerde<>();

        statusSerde.configure(Map.of(URL_CONFIG, this.getSchemaRegistryUrl()), false);
        tripSerde.configure(Map.of(URL_CONFIG, this.getSchemaRegistryUrl()), false);

        builder.stream(
                this.getInputTopics().get(0),
                Consumed.with(stringSerde, statusSerde)
        )
                .groupBy((k, v) -> v.getTripId(), Grouped.with(stringSerde, statusSerde))
                .aggregate(
                        Trip::new,
                        TripAggregationApp::aggregateTrip,
                        Materialized.with(stringSerde, tripSerde)
                )
                .toStream()
                .to(this.getOutputTopic(), Produced.with(stringSerde, tripSerde));
    }

    @Override
    protected Properties createKafkaProperties() {
        final Properties kafkaProperties = super.createKafkaProperties();
        kafkaProperties.setProperty(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.AT_LEAST_ONCE);
        kafkaProperties
                .setProperty(StreamsConfig.producerPrefix(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION), "5");
        return kafkaProperties;
    }

    @Override
    public String getUniqueAppId() {
        return "trip-aggregator";
    }
}
