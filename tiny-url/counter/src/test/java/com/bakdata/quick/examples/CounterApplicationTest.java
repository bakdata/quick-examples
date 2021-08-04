package com.bakdata.quick.examples;

import com.bakdata.fluent_kafka_streams_tests.TestTopology;
import java.util.List;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CounterApplicationTest {
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    private static final String KEY1 = "firstLink";
    private static final String KEY2 = "secondLink";
    private TestTopology<String, Long> testTopology = null;
    private CounterApplication app;

    @BeforeEach
    void init() {
        this.app = createCounterApplication();
        this.testTopology = new TestTopology(app::createTopology, app.getKafkaProperties());
        this.testTopology.start();
    }

    @AfterEach
    void tearDown() {
        this.testTopology.stop();
        this.app.close();
    }

    @Test
    void shouldCountKeyField() {
        this.testTopology.input(INPUT)
                .withSerde(Serdes.String(), Serdes.String())
                .add(KEY1, "")
                .add(KEY1, "")
                .add(KEY2, "");

        this.testTopology.streamOutput(OUTPUT)
                .withSerde(Serdes.String(), Serdes.Long())
                .expectNextRecord()
                .hasKey(KEY1)
                .hasValue(1L)
                .expectNextRecord()
                .hasKey(KEY1)
                .hasValue(2L)
                .expectNextRecord()
                .hasKey(KEY2)
                .hasValue(1L)
                .expectNoMoreRecord();
    }

    private static CounterApplication createCounterApplication() {
        final CounterApplication app = new CounterApplication();
        app.setInputTopics(List.of(INPUT));
        app.setOutputTopic(OUTPUT);
        return app;
    }
}
