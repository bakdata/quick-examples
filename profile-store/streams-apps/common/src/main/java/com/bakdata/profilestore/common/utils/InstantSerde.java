package com.bakdata.profilestore.common.utils;

import java.time.Instant;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

public class InstantSerde extends Serdes.WrapperSerde<Instant> {
    private static final Serializer<Long> longSerializer = new LongSerializer();
    private static final Deserializer<Long> longDeserializer = new LongDeserializer();

    public static class InstantSerializer implements Serializer<Instant> {


        @Override
        public byte[] serialize(final String topic, final Instant data) {
            return longSerializer.serialize(topic, data.toEpochMilli());
        }

    }

    public static class InstantDeserializer implements Deserializer<Instant> {

        @Override
        public Instant deserialize(final String topic, final byte[] data) {
            return Instant.ofEpochMilli(longDeserializer.deserialize(topic, data));
        }

    }

    public InstantSerde() {
        super(new InstantSerializer(), new InstantDeserializer());
    }

}
