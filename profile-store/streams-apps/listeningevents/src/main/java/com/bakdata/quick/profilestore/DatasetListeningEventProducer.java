package com.bakdata.quick.profilestore;

import io.d9p.demo.avro.ListeningEvent;
import java.time.Instant;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.csv.CSVRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import picocli.CommandLine;

/**
 * Produces the listening event dataset.
 * Adds some flexibility to use the dataset as a realtime simulation, in particular
 * looping, sampling (dropout) and realtime timestamps.
 */
@Getter
@Setter
public class DatasetListeningEventProducer extends DataSetProducer<ListeningEvent> {

    @CommandLine.Option(names = "--delay", defaultValue = "0", description = "delay between emissions in seconds")
    protected double delay;

    @CommandLine.Option(names = "--realtime", defaultValue = "false", arity = "0..1",
            description = "replace listening event timestamp by time of emission")
    private boolean realtime;

    @CommandLine.Option(names = "--repeat", defaultValue = "1", description = "Repeat the dataset n times")
    private long repetitions;

    @CommandLine.Option(names = "--dropout", defaultValue = "0.0", description = "Randomly drop a ratio of the dataset")
    private double dropout;

    protected long randomseed = 158;

    public static void main(final String[] args) {
        startApplication(new DatasetListeningEventProducer(), args);
    }

    @Override
    public void produceDataset(final String topic, final Producer<Long, ListeningEvent> producer)
            throws Exception {
        final Random random = new Random(this.randomseed);
        for (long repetitions = this.repetitions; repetitions > 0; repetitions--) {
            for (final CSVRecord record : this.getDataset()) {
                if (random.nextDouble() >= this.dropout) {
                    this.produceSingleRecord(this.makeKafkaRecord(this.outputTopic, record), producer);
                }
            }
        }
    }

    @Override
    public void produceSingleRecord(final ProducerRecord<Long, ListeningEvent> record,
            final Producer<Long, ListeningEvent> producer) throws Exception {
        producer.send(record);
        Thread.sleep((long) (this.delay * 1000));

    }

    @Override
    protected ProducerRecord<Long, ListeningEvent> makeKafkaRecord(final String topic, final CSVRecord record) {
        if (record.size() < 5) {
            throw new IllegalStateException("Cannot parse line.");
        }
        final ListeningEvent listeningEventRecord = ListeningEvent.newBuilder()
                .setUserId(Long.parseLong(record.get(0)))
                .setArtistId(Long.parseLong(record.get(1)))
                .setAlbumId(Long.parseLong(record.get(2)))
                .setTrackId(Long.parseLong(record.get(3)))
                .setTimestamp(this.realtime ? Instant.now() : Instant.ofEpochSecond(Long.parseLong(record.get(4))))
                .build();
        return new ProducerRecord<>(topic, listeningEventRecord.getUserId(), listeningEventRecord);
    }


}
