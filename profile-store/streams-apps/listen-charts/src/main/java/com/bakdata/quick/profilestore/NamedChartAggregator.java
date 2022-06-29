package com.bakdata.quick.profilestore;

import io.d9p.demo.avro.NamedChartRecord;
import io.d9p.demo.avro.NamedCharts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NamedChartAggregator extends ChartAggregationWrapper<NamedCharts, NamedChartRecord> {

    public NamedChartAggregator(final long k) {
        super(k);
    }

    @Override
    public NamedCharts initialize() {
        return new NamedCharts(new ArrayList<>());
    }

    @Override
    public Comparator<NamedChartRecord> getComparator() {
        return Comparator.comparingLong(NamedChartRecord::getCountPlays).reversed()
                .thenComparing(NamedChartRecord::getName).thenComparing(NamedChartRecord::getId);
    }

    @Override
    public Comparator<NamedChartRecord> getSameIdComparator() {
        return Comparator.comparingLong(NamedChartRecord::getCountPlays);
    }

    @Override
    public Long getId(final NamedChartRecord chartRecord) {
        return chartRecord.getId();
    }

    @Override
    public List<NamedChartRecord> getTopK(final NamedCharts charts) {
        return charts.getTopK();
    }

    @Override
    public void setTopK(final NamedCharts oldCharts, final List<NamedChartRecord> newChartList) {
        oldCharts.setTopK(newChartList);
    }
}
