package com.bakdata.quick.profilestore;

import com.bakdata.kafka.KafkaProducerApplication;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import picocli.CommandLine;

@Slf4j
@Setter
public abstract class DataSetProducer<ValueType extends SpecificRecord> extends KafkaProducerApplication {

    @CommandLine.Option(names = "--dataset", description = "path to dataset file (csv-formatted)")
    protected File dataset;

    @CommandLine.Option(names = "--sep", defaultValue = "\t", description = "csv separator")
    protected char separator;

    private void checkState() {
        if (this.dataset != null && !this.dataset.exists()) {
            throw new RuntimeException("Dataset not found");
        }
    }

    @Override
    protected void runApplication() {
        this.checkState();
        try {
            this.produceDataset(this.outputTopic, this.createProducer());
        } catch (final Exception e) {
            log.warn("Error during production", e);
        }
    }

    public void produceDataset(final String topic, final Producer<Long, ValueType> eventProducer) throws Exception {
        log.debug("Start producing...");
        for (final CSVRecord csvRecord : this.getDataset()) {
            this.produceSingleRecord(this.makeKafkaRecord(topic, csvRecord), eventProducer);
        }
    }

    public void produceSingleRecord(final ProducerRecord<Long, ValueType> record,
            final Producer<Long, ValueType> eventProducer) throws Exception {
        eventProducer.send(record);
    }

    @Override
    protected Properties createKafkaProperties() {
        final Properties props = super.createKafkaProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        return props;
    }

    private CSVFormat getFormat() {
        return CSVFormat.TDF.withDelimiter(this.separator).withRecordSeparator('\n');
    }

    protected CSVParser getDataset() throws Exception {
        log.debug("Read from dataset {}", this.dataset);
        final Reader in = new BufferedReader(
                (this.dataset == null) ? new InputStreamReader(System.in) : new FileReader(this.dataset));
        return this.getFormat().parse(in);
    }

    protected abstract ProducerRecord<Long, ValueType> makeKafkaRecord(final String topic, final CSVRecord record);

}
