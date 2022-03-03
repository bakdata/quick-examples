package com.bakdata.quick.examples.tinyurl.counter;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig.defaultClusterConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.schemaregistrymock.junit5.SchemaRegistryMockExtension;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.ReadKeyValues;
import net.mguenther.kafka.junit.SendKeyValuesTransactional;
import net.mguenther.kafka.junit.TopicConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CounterApplicationIntegrationTest {
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    private static final int TIMEOUT_SECONDS = 5;
    private CounterApplication app = null;
    private EmbeddedKafkaCluster kafka = null;

    @RegisterExtension
    final SchemaRegistryMockExtension schemaRegistry = new SchemaRegistryMockExtension();

    @BeforeEach
    void init() {
        this.kafka = provisionWith(defaultClusterConfig());
        this.kafka.start();
        this.kafka.createTopic(TopicConfig.withName(INPUT).useDefaults());
        this.kafka.createTopic(TopicConfig.withName(OUTPUT).useDefaults());
        assertThat(this.kafka.exists(INPUT)).isTrue();
        assertThat(this.kafka.exists(OUTPUT)).isTrue();
        this.createCounterApplication();
    }

    @AfterEach
    void tearDown() {
        this.app.close();
        this.kafka.stop();
    }

    @Test
    /*
      Kafka is caching the count. You need to remove the cache manually
      rm -rf /tmp/kafka-streams/tiny-url-counter-input
     */
    void shouldWaitForRecordsToBePublished() throws InterruptedException {
        final List<KeyValue<String, String>> records =
                List.of(new KeyValue<>("foo", ""), new KeyValue<>("foo", ""));

        final SendKeyValuesTransactional<String, String> transactionalBuilder =
                SendKeyValuesTransactional.inTransaction(INPUT, records)
                        .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                        .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                        .build();
        this.kafka.send(transactionalBuilder);
        Thread.sleep(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

        assertThat(this.kafka.read(ReadKeyValues.from(OUTPUT, String.class, Long.class)
                .with(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
                .with(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class)
                .build()))
                .containsExactly(new KeyValue<>("foo", 2L));
    }

    private void createCounterApplication() {
        this.app = new CounterApplication();
        this.app.setProductive(false);
        this.app.setBrokers(this.kafka.getBrokerList());
        this.app.setSchemaRegistryUrl(this.schemaRegistry.getUrl());
        this.app.setInputTopics(List.of(INPUT));
        this.app.setOutputTopic(OUTPUT);
        this.app.run();
    }
}
