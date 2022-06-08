package com.bakdata.profilestore.recommender.rest;

import com.bakdata.profilestore.common.FieldType;
import com.bakdata.profilestore.recommender.algorithm.Salsa;
import com.bakdata.profilestore.recommender.graph.BipartiteGraph;
import io.d9p.demo.avro.Item;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

@Slf4j
@Path("/recommendation")
public class RestResource {
    private final Map<FieldType, BipartiteGraph> graphs;
    private final KafkaStreams streams;
    private final Map<FieldType, String> storeNames;
    private final boolean idResolution;

    public RestResource(final Map<FieldType, BipartiteGraph> graphs,
            final KafkaStreams streams,
            final Map<FieldType, String> storeNames,
            final boolean idResolution) {
        this.graphs = graphs;
        this.streams = streams;
        this.storeNames = storeNames;
        this.idResolution = idResolution;
    }

    /**
     * Gets a list of recommendations for an id
     *
     * @param userId the id of the user the recommendations are made for
     * @param limit number of recommendations
     * @param walks number of random walks in the monte carlo simulation
     * @param walkLength number of steps in a random walk
     * @param resetProbability probability to jump back to query node
     * @return List of size limit with ids for recommendations as elements
     */
    @GET
    @Path("/{userId}/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Recommendations getRecommendationsForUser(
            @PathParam("userId") final long userId,
            @PathParam("type") final String type,
            @DefaultValue("10") @QueryParam("limit") final int limit,
            @DefaultValue("1000") @QueryParam("walks") final int walks,
            @DefaultValue("100") @QueryParam("walkLength") final int walkLength,
            @DefaultValue("0.1") @QueryParam("resetProbability") final float resetProbability) {
        log.info("Request for user {} and type {}", userId, type);

        final FieldType recommendationType = FieldType.valueOf(type.toUpperCase());
        final Salsa salsa = new Salsa(this.graphs.get(recommendationType), new Random());
        final List<Long> ids = RestResource.computeRecommendations(salsa, userId, limit, walks, walkLength, resetProbability);

        if (this.idResolution) {
            // store is backed by a GlobalKTable
            final ReadOnlyKeyValueStore<Long, Item> nameTable =
                    this.streams.store(StoreQueryParameters.fromNameAndType(this.storeNames.get(recommendationType),
                            QueryableStoreTypes.keyValueStore()));

            return new Recommendations.Named(ids.stream().map(nameTable::get).collect(Collectors.toList()));
        }
        return new Recommendations.Unnamed(ids);
    }

    private static List<Long> computeRecommendations(final Salsa salsa,
            final long userId,
            final int limit,
            final int walks,
            final int walkLength,
            final float resetProbability) {
        try {
            return salsa.compute(userId, walks, walkLength, resetProbability, limit);
        } catch (final RuntimeException e) {
            log.info("No recommendation computed", e);
            return Collections.emptyList();
        }
    }

    /**
     * Wrapper for the named/ unnamed recommendations. This is necessary 1) to embed the list in a recommendations
     * field, as required by the schema 2) to embed the ids in an object, such that a topic directive can enter there 3)
     * to expose only id and name for the avro item
     */
    private static class Recommendations {
        @Getter
        final List<Object> recommendations;

        private Recommendations(final List<Object> recommendations) {
            this.recommendations = recommendations;
        }

        private static final class Named extends Recommendations {
            @Getter
            static final class Recommendation {
                final Long id;
                final String name;

                private Recommendation(final io.d9p.demo.avro.Item item) {
                    this.id = item.getId();
                    this.name = item.getName();
                }
            }

            private Named(final List<io.d9p.demo.avro.Item> items) {
                super(items.stream().map(Named.Recommendation::new).collect(Collectors.toList()));
            }
        }

        // plain ids
        private static final class Unnamed extends Recommendations {
            @Getter
            static final class Recommendation {
                final Long id;

                private Recommendation(final Long id) {
                    this.id = id;
                }
            }

            private Unnamed(final List<Long> items) {
                super(items.stream().map(Unnamed.Recommendation::new).collect(Collectors.toList()));
            }
        }


    }
}
