package com.bakdata.profilestore.listeningevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import io.d9p.demo.avro.ListeningEvent;
import com.bakdata.schemaregistrymock.junit5.SchemaRegistryMockExtension;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import java.io.File;
import java.io.StringReader;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


class DatasetListeningEventProducerTest {

    private MockProducer<Long, ListeningEvent> internalKafkaProducer;
    private DatasetListeningEventProducer listeningEventProducer;

    @RegisterExtension
    final SchemaRegistryMockExtension schemaRegistryMockExtension = new SchemaRegistryMockExtension();


    @BeforeEach
    public void setUp() {
        final Map<String, String> serdeConfig = Collections.singletonMap(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.schemaRegistryMockExtension.getUrl());
        final SpecificAvroSerializer<ListeningEvent> specificAvroSerializer = new SpecificAvroSerializer<>();
        specificAvroSerializer.configure(serdeConfig, false);
        this.internalKafkaProducer = new MockProducer<>(true, Serdes.Long().serializer(),
                specificAvroSerializer);
        this.listeningEventProducer = this.createProducerBase();
    }

    @Test
    void testEntireTableIsProduced() throws Exception {
        this.listeningEventProducer.produceDataset("test-topic", this.internalKafkaProducer);
        assertEquals(1000, this.internalKafkaProducer.history().size());
    }

    @Test
    void testRealTimeTS() throws Exception {
        this.listeningEventProducer.setRealtime(true);
        final List<ProducerRecord<Long, ListeningEvent>> testRecords = this.makeTestRecords();
        assertEquals(Instant.now().toEpochMilli(), testRecords.get(0).value().getTimestamp().toEpochMilli(), 1000);
    }

    @Test
    void testMessagesAreDelayed() throws Exception {
        this.listeningEventProducer.setDelay(1.5);
        final List<ProducerRecord<Long, ListeningEvent>> testRecords = this.makeTestRecords();
        long t0 = System.currentTimeMillis();
        for (final ProducerRecord<Long, ListeningEvent> testRecord : testRecords) {
            this.listeningEventProducer.produceSingleRecord(testRecord, this.internalKafkaProducer);
        }
        final double productionSeconds = (double) (System.currentTimeMillis() - t0) / 1000;
        assertEquals(this.internalKafkaProducer.history().size(), testRecords.size());
        assertEquals(this.listeningEventProducer.getDelay() * testRecords.size(), productionSeconds, 0.5);

    }

    @Test
    void testLoopingStopsAfterRepetitions() throws Exception {
        this.listeningEventProducer.setRepetitions(2);
        this.listeningEventProducer.produceDataset("test-topic", this.internalKafkaProducer);
        assertEquals(2000, this.internalKafkaProducer.history().size());
    }

    @Test
    void testDropOut() throws Exception {
        this.listeningEventProducer.setRepetitions(2);
        this.listeningEventProducer.setDropout(.8);
        final long keep = new Random(this.listeningEventProducer.getRandomseed()).doubles(2000).filter(r -> r > .8).count();
        this.listeningEventProducer.produceDataset("test-topic", this.internalKafkaProducer);
        assertEquals(keep, this.internalKafkaProducer.history().size());
    }

    private DatasetListeningEventProducer createProducerBase() {
        final DatasetListeningEventProducer datasetListeningEventProducer = new DatasetListeningEventProducer();
        datasetListeningEventProducer.setBrokers("localhost:1111");
        datasetListeningEventProducer.setSchemaRegistryUrl("sth://localhost:1234");
        datasetListeningEventProducer.setRepetitions(1);
        datasetListeningEventProducer.setSeparator('\t');
        datasetListeningEventProducer.setDataset(
                new File(Objects.requireNonNull(this.getClass().getResource("/LFM-1b-sample/LFM-1b_LEs-sample.txt"))
                        .getPath()));
        return datasetListeningEventProducer;
    }

    private List<ProducerRecord<Long, ListeningEvent>> makeTestRecords() throws Exception {
        final CSVParser parser = CSVFormat.TDF.parse(new StringReader(
                "31435741\t65\t125\t231\t1361097922\n"
                        + "31435741\t10\t431\t123\t1361098554\n"
                        + "21957392\t99\t119\t67\t13611209845"));
        return parser.getRecords().stream()
                .map(csvrecord -> this.listeningEventProducer.makeKafkaRecord("test-topic", csvrecord)).collect(
                        Collectors.toList());
    }


}
