import io.d9p.demo.avro.ChartRecord;
import io.d9p.demo.avro.Charts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AnonymousChartAggregator extends ChartAggregationWrapper<Charts, ChartRecord> {
    public AnonymousChartAggregator(final long k) {
        super(k);
    }

    @Override
    public Charts initialize() {
        return new Charts(new ArrayList<>());
    }

    @Override
    public Comparator<ChartRecord> getComparator() {
        return Comparator.comparingLong(ChartRecord::getCountPlays).reversed()
                .thenComparing(ChartRecord::getId);
    }

    @Override
    public Comparator<ChartRecord> getSameIdComparator() {
        return Comparator.comparingLong(ChartRecord::getCountPlays);
    }

    @Override
    public Long getId(final ChartRecord chartRecord) {
        return chartRecord.getId();
    }

    @Override
    public List<ChartRecord> getTopK(final Charts charts) {
        return charts.getTopK();
    }

    @Override
    public void setTopK(final Charts oldCharts, final List<ChartRecord> newChartList) {
        oldCharts.setTopK(newChartList);

    }
}
