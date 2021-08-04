package com.bakdata.quick.examples;

import com.bakdata.kafka.KafkaStreamsApplication;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

@Slf4j
public class CounterApplication extends KafkaStreamsApplication {

    public static void main(final String[] args) {
        log.info("Starting counter application with args:{}", Arrays.toString(args));
        KafkaStreamsApplication.startApplication(new CounterApplication(), args);
    }

    @Override
    public void buildTopology(final StreamsBuilder builder) {

        final KStream<String, String> inputStream =
                builder.stream(this.getInputTopic(), Consumed.with(Serdes.String(), Serdes.String()));

        inputStream
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .count()
                .toStream()
                .to(this.outputTopic, Produced.with(Serdes.String(), Serdes.Long()));
    }

    @Override
    public String getUniqueAppId() {
        return String.format("tiny-url-counter-%s", this.getInputTopic());
    }
}
