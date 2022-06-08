package com.bakdata.profilestore.recommender;

import com.bakdata.profilestore.common.FieldType;
import com.bakdata.profilestore.recommender.avro.AdjacencyList;
import com.bakdata.profilestore.recommender.graph.WritableKeyValueGraph;
import com.bakdata.profilestore.recommender.graph.WriteableBipartiteGraph;
import io.d9p.demo.avro.ListeningEvent;
import java.util.EnumMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

/**
 * Processor updates the left and right index for the random walks
 */
@Slf4j
public class RecommenderProcessor implements Processor<Long, ListeningEvent, Long, AdjacencyList> {
    private final EnumMap<FieldType, WriteableBipartiteGraph> graphs =
            new EnumMap<>(FieldType.class);


    @Override
    public void init(final ProcessorContext<Long, AdjacencyList> context) {
        for (final FieldType type : FieldType.values()) {
            final KeyValueStore<Long, AdjacencyList> leftIndex = context
                    .getStateStore(getLeftIndexName(type));
            final KeyValueStore<Long, AdjacencyList> rightIndex = context
                    .getStateStore(getRightIndexName(type));

            final WriteableBipartiteGraph graph = new WritableKeyValueGraph(leftIndex, rightIndex);

            this.graphs.put(type, graph);
        }
    }

    @Override
    public void process(final Record<Long, ListeningEvent> record) {
        final ListeningEvent event = record.value();
        this.graphs.get(FieldType.ALBUM).addEdge(event.getUserId(), event.getAlbumId());
        this.graphs.get(FieldType.ARTIST).addEdge(event.getUserId(), event.getArtistId());
        this.graphs.get(FieldType.TRACK).addEdge(event.getUserId(), event.getTrackId());

    }

    @Override
    public void close() {
    }


    public static String getLeftIndexName(final FieldType fieldType) {
        return String.format("%s_%s", fieldType, RecommenderMain.LEFT_INDEX_NAME);
    }

    public static String getRightIndexName(final FieldType fieldType) {
        return String.format("%s_%s", fieldType, RecommenderMain.RIGHT_INDEX_NAME);
    }
}
