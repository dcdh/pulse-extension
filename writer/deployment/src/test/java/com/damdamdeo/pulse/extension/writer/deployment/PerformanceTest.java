package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.projection.Projection;
import com.damdamdeo.pulse.extension.core.projection.ProjectionFromEventStore;
import com.damdamdeo.pulse.extension.core.projection.SingleResultAggregateQuery;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.context.api.ManagedExecutorConfig;
import jakarta.inject.Inject;
import org.HdrHistogram.Histogram;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.LongStream;

class PerformanceTest {

    private static ExecutedBy BOB = new ExecutedBy.EndUser("bob", true);

    static final Logger LOGGER = Logger.getLogger(PerformanceTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.log.category.\"com.damdamdeo.pulse.extension.writer.runtime.projection\".level", "INFO");

    // https://github.com/quarkusio/quarkus/issues/19676
    @Inject
    @CacheName("passphrase")
    Cache cache;

    @Inject
    EventRepository<Todo, TodoId> todoEventRepository;

    @Inject
    ProjectionFromEventStore<TodoProjection> todoProjectionProjectionFromEventStore;

    @Inject
    DataSource dataSource;

    record TodoProjection(TodoId todoId,
                          String description,
                          Status status,
                          boolean important) implements Projection {
    }

    public static final class TodoProjectionSingleResultAggregateQuery implements SingleResultAggregateQuery {

        @Override
        public String query(final Passphrase passphrase, final AggregateId aggregateId) {
            // language=sql
            return """
                    WITH decrypted AS (
                      SELECT
                        aggregate_root_id,
                        aggregate_root_type,
                        public.pgp_sym_decrypt(aggregate_root_payload, '%1$s')::jsonb AS decrypted_aggregate_root_payload,
                        belongs_to
                      FROM t_aggregate_root
                      WHERE belongs_to = '%2$s'
                    )
                    SELECT jsonb_build_object(
                      'todoId', d.decrypted_aggregate_root_payload -> 'id',
                      'description', d.decrypted_aggregate_root_payload ->> 'description',
                      'status', d.decrypted_aggregate_root_payload ->> 'status',
                      'important', d.decrypted_aggregate_root_payload ->> 'important',
                      'checklist', COALESCE(
                        jsonb_agg(
                          i.decrypted_aggregate_root_payload::jsonb
                        ) FILTER (WHERE i.aggregate_root_id IS NOT NULL),
                        '[]'::jsonb
                      )
                    ) AS response
                    FROM decrypted d
                    LEFT JOIN decrypted i
                      ON i.belongs_to = d.aggregate_root_id
                     AND i.aggregate_root_type = 'com.damdamdeo.pulse.extension.core.TodoChecklist'
                    WHERE d.aggregate_root_type = 'com.damdamdeo.pulse.extension.core.Todo'
                      AND d.aggregate_root_id = '%2$s'
                    GROUP BY d.aggregate_root_id, d.aggregate_root_type, d.decrypted_aggregate_root_payload, d.belongs_to;
                    """.formatted(new String(passphrase.passphrase()), aggregateId.id());
        }
    }

    private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    @Inject
    @ManagedExecutorConfig(maxAsync = 100)
    ManagedExecutor managedExecutor;

    private static final class FindByTaskSupplier implements Supplier<Long> {

        private final ProjectionFromEventStore<TodoProjection> todoProjectionProjectionFromEventStore;
        private final Long sequence;

        private FindByTaskSupplier(final ProjectionFromEventStore<TodoProjection> todoProjectionProjectionFromEventStore,
                                   final Long sequence) {
            this.todoProjectionProjectionFromEventStore = Objects.requireNonNull(todoProjectionProjectionFromEventStore);
            this.sequence = Objects.requireNonNull(sequence);
        }

        @Override
        public Long get() {
            final Instant start = Instant.now();
            todoProjectionProjectionFromEventStore.findBy(new OwnedBy("Performance"), new TodoId("Performance", sequence), new TodoProjectionSingleResultAggregateQuery());
            return Duration.between(start, Instant.now()).toMillis();
        }
    }

    @Test
    @Disabled
        // Can't go more ... Quarkus limit test running to 5 min max, and it cannot be overridden...
    void executePerfTest() {
        LOGGER.info("Start execute performance test");
        LOGGER.info("Create 30.000 of NewTodoCreated events");
        final StopWatch watch = StopWatch.createStarted();
        for (long i = 0; i < 30_000; i++) {
            if (i % 1_000 == 0) {
                LOGGER.info("Current creation %d".formatted(i));
            }
            final TodoId givenTodoId = new TodoId("Performance", i);
            final List<VersionizedEvent> givenTodoEvents = List.of(
                    new VersionizedEvent(new AggregateVersion(0),
                            new NewTodoCreated(LOREM_IPSUM)));
            todoEventRepository.save(givenTodoEvents,
                    new Todo(
                            givenTodoId,
                            LOREM_IPSUM,
                            Status.IN_PROGRESS,
                            false
                    ), BOB);
        }
        watch.stop();
        LOGGER.info("30.000 of NewTodoCreated events created in %d ms".formatted(watch.getTime(TimeUnit.MILLISECONDS)));
        watch.reset();

        final List<FindByTaskSupplier> taskSuppliers = LongStream.range(0, 1000)
                .mapToObj(sequenceNb -> new FindByTaskSupplier(todoProjectionProjectionFromEventStore, sequenceNb))
                .toList();
        final List<CompletableFuture<Long>> futures = taskSuppliers.stream()
                .map(supplier -> CompletableFuture.supplyAsync(supplier, managedExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        final List<Long> executionsIn = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        final Histogram histogram = new Histogram(5);
        executionsIn.forEach(histogram::recordValue);

        LOGGER.info("Fetch Performance");
        LOGGER.info("min: %d in ms".formatted(histogram.getMinValue()));
        LOGGER.info("P50: %d in ms".formatted(histogram.getValueAtPercentile(50)));
        LOGGER.info("P90: %d in ms".formatted(histogram.getValueAtPercentile(90)));
        LOGGER.info("P99: %d in ms".formatted(histogram.getValueAtPercentile(99)));
        LOGGER.info("max: %d in ms".formatted(histogram.getMaxValue()));

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement perfEventTablePreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT pg_size_pretty(pg_table_size('t_event')) AS table_size,
                                     pg_size_pretty(pg_indexes_size('t_event')) AS indexes_size,
                                     pg_size_pretty(pg_total_relation_size('t_event')) AS total_size
                             """
             );
             final PreparedStatement perfAggregateRootTablePreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT pg_size_pretty(pg_table_size('t_aggregate_root')) AS table_size,
                                     pg_size_pretty(pg_indexes_size('t_aggregate_root')) AS indexes_size,
                                     pg_size_pretty(pg_total_relation_size('t_aggregate_root')) AS total_size
                             """
             );
             final ResultSet perfEventTableResultSet = perfEventTablePreparedStatement.executeQuery();
             final ResultSet perfAggregateRootTableResultSet = perfAggregateRootTablePreparedStatement.executeQuery();
        ) {
            perfEventTableResultSet.next();
            LOGGER.info("t_event - table_size '%s' - indexes_size '%s' - total_size '%s'"
                    .formatted(perfEventTableResultSet.getString("table_size"),
                            perfEventTableResultSet.getString("indexes_size"),
                            perfEventTableResultSet.getString("total_size")));
            perfAggregateRootTableResultSet.next();
            LOGGER.info("t_event - table_size '%s' - indexes_size '%s' - total_size '%s'"
                    .formatted(perfAggregateRootTableResultSet.getString("table_size"),
                            perfAggregateRootTableResultSet.getString("indexes_size"),
                            perfAggregateRootTableResultSet.getString("total_size")));
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }
/*
2025-11-15 19:31:53,061 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Start execute performance test
2025-11-15 19:31:53,061 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Create 30.000 of NewTodoCreated events
2025-11-15 19:31:53,062 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 0
2025-11-15 19:32:01,108 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 1000
2025-11-15 19:32:08,714 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 2000
2025-11-15 19:32:15,380 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 3000
2025-11-15 19:32:26,337 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 4000
2025-11-15 19:32:33,457 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 5000
2025-11-15 19:32:40,545 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 6000
2025-11-15 19:32:47,396 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 7000
2025-11-15 19:32:54,437 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 8000
2025-11-15 19:33:01,396 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 9000
2025-11-15 19:33:08,970 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 10000
2025-11-15 19:33:15,910 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 11000
2025-11-15 19:33:28,440 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 12000
2025-11-15 19:33:35,272 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 13000
2025-11-15 19:33:42,618 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 14000
2025-11-15 19:33:49,720 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 15000
2025-11-15 19:33:56,997 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 16000
2025-11-15 19:34:04,336 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 17000
2025-11-15 19:34:11,540 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 18000
2025-11-15 19:34:18,960 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 19000
2025-11-15 19:34:29,991 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 20000
2025-11-15 19:34:37,180 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 21000
2025-11-15 19:34:43,938 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 22000
2025-11-15 19:34:51,302 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 23000
2025-11-15 19:34:58,485 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 24000
2025-11-15 19:35:05,985 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 25000
2025-11-15 19:35:13,425 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 26000
2025-11-15 19:35:20,292 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 27000
2025-11-15 19:35:27,401 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 28000
2025-11-15 19:35:38,324 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Current creation 29000
2025-11-15 19:35:45,172 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) 30.000 of NewTodoCreated events created in 232109 ms
2025-11-15 19:35:46,836 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) Fetch Performance
2025-11-15 19:35:46,836 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) min: 6 in ms
2025-11-15 19:35:46,836 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) P50: 21 in ms
2025-11-15 19:35:46,836 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) P90: 92 in ms
2025-11-15 19:35:46,836 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) P99: 1542 in ms
2025-11-15 19:35:46,836 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) max: 1620 in ms
2025-11-15 19:35:46,838 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) t_event - table_size '20 MB' - indexes_size '4616 kB' - total_size '24 MB'
2025-11-15 19:35:46,838 INFO  [com.dam.pul.ext.wri.dep.PerformanceTest] (main) t_event - table_size '21 MB' - indexes_size '2072 kB' - total_size '23 MB'
*/
}
