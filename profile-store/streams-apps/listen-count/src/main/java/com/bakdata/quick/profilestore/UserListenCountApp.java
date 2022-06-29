package com.bakdata.quick.profilestore;

import com.bakdata.kafka.KafkaStreamsApplication;
import io.d9p.demo.avro.ListeningEvent;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

/**
 * Counts the listening events per user
 */
public class UserListenCountApp extends KafkaStreamsApplication {

    public static void main(final String[] args) {
        startApplication(new UserListenCountApp(), args);
    }

    @Override
    public void buildTopology(final StreamsBuilder builder) {

        final KStream<Long, ListeningEvent> inputStream = builder.stream(this.getInputTopic());

        inputStream
                .groupByKey()
                .count()
                .toStream()
                .to(this.getOutputTopic(), Produced.valueSerde(Serdes.Long()));
    }

    @Override
    public Properties createKafkaProperties() {
        final Properties kafkaConfig = super.createKafkaProperties();
        kafkaConfig.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Long().getClass().getName());
        return kafkaConfig;
    }

    @Override
    public String getUniqueAppId() {
        return "user-listen-count";
    }
}
