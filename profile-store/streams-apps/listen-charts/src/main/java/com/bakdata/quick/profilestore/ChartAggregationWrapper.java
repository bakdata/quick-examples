package com.bakdata.quick.profilestore;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jooq.lambda.Seq;

/**
 * Wraps the types ChartT and ChartRecordT, since we have no polymorphism in avro
 *
 * @param <ChartT>
 * @param <ChartRecordT>
 */
public abstract class ChartAggregationWrapper<ChartT, ChartRecordT> {
    long k;

    protected ChartAggregationWrapper(final long k) {
        this.k = k;
    }

    public abstract ChartT initialize();

    /**
     * Updates the list of top k chart records with a new record
     *
     * @param userId the id of the user
     * @param currentRecord the new record
     * @param currentCharts the current top k charts
     * @return the updated charts, maybe containing the new record
     */
    public ChartT update(final Long userId, final ChartRecordT currentRecord, final ChartT currentCharts) {
        final List<ChartRecordT> updatedCharts =
                Seq.concat(this.getTopK(currentCharts).stream(), Stream.of(currentRecord))
                        // if there are two records with the same id, remove the one with the smaller count
                        .grouped(this::getId)
                        .map(tuple -> tuple.v2().max(this.getSameIdComparator()).get())
                        .sorted(this.getComparator())
                        .limit(k)
                        .collect(Collectors.toList());
        this.setTopK(currentCharts, updatedCharts);
        return currentCharts;
    }

    public abstract Comparator<ChartRecordT> getComparator();

    public abstract Comparator<ChartRecordT> getSameIdComparator();

    public abstract Long getId(final ChartRecordT chartRecord);

    public abstract List<ChartRecordT> getTopK(final ChartT charts);

    public abstract void setTopK(final ChartT oldCharts, final List<ChartRecordT> newChartList);

}
