import com.bakdata.kafka.KafkaStreamsApplication;
import com.bakdata.profilestore.common.utils.InstantSerde;
import io.d9p.demo.avro.ListeningEvent;
import java.time.Instant;
import java.util.Properties;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import picocli.CommandLine;

public class UserListenActivity extends KafkaStreamsApplication {

    public enum Kind {
        FIRST,
        LAST
    }

    @Getter
    @Setter
    @CommandLine.Option(names = "--kind", required = true, description = "Track first/last listening event")
    private Kind kind;

    public static void main(final String[] args) {
        startApplication(new UserListenActivity(), args);
    }

    @Override
    public void buildTopology(final StreamsBuilder builder) {
        final KStream<Long, ListeningEvent> inputStream =
                builder.stream(this.getInputTopic());

        inputStream
                .groupByKey()
                .aggregate(this::initial, this::compare, Materialized.with(Serdes.Long(), new InstantSerde()))
                .toStream()
                .to(this.outputTopic, Produced.with(Serdes.Long(), new InstantSerde()));
    }

    @Override
    public String getUniqueAppId() {
        return "user-listen-activity-" + this.kind.name().toLowerCase();
    }

    private Instant initial() {
        return this.kind == Kind.FIRST ? Instant.MAX : Instant.MIN;
    }

    private Instant compare(final Long userId, final ListeningEvent listeningEvent, final Instant currentBest) {
        final Instant other = listeningEvent.getTimestamp();
        if (this.kind == Kind.FIRST) {
            return other.isBefore(currentBest) ? other : currentBest;
        } else {
            return other.isBefore(currentBest) ? currentBest : other;
        }
    }

    public Properties createKafkaProperties() {
        final Properties kafkaConfig = super.createKafkaProperties();
        kafkaConfig.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Long().getClass().getName());
        return kafkaConfig;
    }
}
