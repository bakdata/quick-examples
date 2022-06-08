package com.bakdata.profilestore.fieldtables;

import com.bakdata.profilestore.common.DataSetProducer;
import io.d9p.demo.avro.Item;
import org.apache.commons.csv.CSVRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

public class DataSetFieldTableProducer extends DataSetProducer<Item> {

    public static void main(final String[] args) {
        startApplication(new DataSetFieldTableProducer(), args);
    }

    @Override
    protected ProducerRecord<Long, Item> makeKafkaRecord(final String topic, final CSVRecord record) {
        if (record.size() < 2) {
            throw new IllegalStateException("Cannot parse line.");
        }
        final Item fieldRecord = new Item(Long.parseLong(record.get(0)), record.get(1));
        return new ProducerRecord<>(topic, fieldRecord.getId(), fieldRecord);
    }
}
