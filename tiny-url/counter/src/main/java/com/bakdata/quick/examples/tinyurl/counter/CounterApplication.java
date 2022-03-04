package com.bakdata.quick.examples.tinyurl.counter;

import com.bakdata.kafka.KafkaStreamsApplication;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CounterApplication extends KafkaStreamsApplication {
    private static final Logger logger = LoggerFactory.getLogger(CounterApplication.class);

    public static void main(final String[] args) {
        logger.info("Starting TinyURL counter application");
        KafkaStreamsApplication.startApplication(new CounterApplication(), args);
    }

    @Override
    public void buildTopology(final StreamsBuilder builder) {
        logger.debug("Setting input topic: {}", this.getInputTopics());

        final KStream<String, String> inputStream =
                builder.stream(this.getInputTopics(), Consumed.with(Serdes.String(), Serdes.String()));

        inputStream
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .count()
                .toStream()
                .to(this.outputTopic, Produced.with(Serdes.String(), Serdes.Long()));
    }

    @Override
    public String getUniqueAppId() {
        return String.format("tiny-url-counter-%s", this.getInputTopics().get(0));
    }
}
