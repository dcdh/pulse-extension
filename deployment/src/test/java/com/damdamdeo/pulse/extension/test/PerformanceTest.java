package com.damdamdeo.pulse.extension.test;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.event.EventRepository;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
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

    static final Logger LOGGER = Logger.getLogger(PerformanceTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.log.category.\"com.damdamdeo.pulse.extension.runtime.projection\".level", "INFO");

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
                        pgp_sym_decrypt(aggregate_root_payload, '%1$s')::jsonb AS decrypted_aggregate_root_payload,
                        in_relation_with
                      FROM t_aggregate_root
                      WHERE in_relation_with = '%2$s'
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
                      ON i.in_relation_with = d.aggregate_root_id
                     AND i.aggregate_root_type = 'com.damdamdeo.pulse.extension.core.TodoChecklist'
                    WHERE d.aggregate_root_type = 'com.damdamdeo.pulse.extension.core.Todo'
                      AND d.aggregate_root_id = '%2$s'
                    GROUP BY d.aggregate_root_id, d.aggregate_root_type, d.decrypted_aggregate_root_payload, d.in_relation_with;
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
            final List<VersionizedEvent<TodoId>> givenTodoEvents = List.of(
                    new VersionizedEvent<>(new AggregateVersion(0),
                            new NewTodoCreated(givenTodoId, LOREM_IPSUM)));
            todoEventRepository.save(givenTodoEvents,
                    new Todo(
                            new TodoId("Performance", i),
                            LOREM_IPSUM,
                            Status.IN_PROGRESS,
                            false
                    ));
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
2025-10-28 23:04:26,303 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Start execute performance test
2025-10-28 23:04:26,304 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Create 30.000 of NewTodoCreated events
2025-10-28 23:04:26,305 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 0
2025-10-28 23:04:31,745 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 1000
2025-10-28 23:04:39,333 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 2000
2025-10-28 23:04:44,283 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 3000
2025-10-28 23:04:48,526 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 4000
2025-10-28 23:04:53,505 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 5000
2025-10-28 23:04:58,601 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 6000
2025-10-28 23:05:08,204 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 7000
2025-10-28 23:05:13,937 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 8000
2025-10-28 23:05:19,531 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 9000
2025-10-28 23:05:25,735 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 10000
2025-10-28 23:05:33,095 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 11000
2025-10-28 23:05:40,857 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 12000
2025-10-28 23:05:47,504 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 13000
2025-10-28 23:05:53,548 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 14000
2025-10-28 23:06:00,107 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 15000
2025-10-28 23:06:09,102 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 16000
2025-10-28 23:06:16,018 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 17000
2025-10-28 23:06:22,619 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 18000
2025-10-28 23:06:29,064 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 19000
2025-10-28 23:06:35,603 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 20000
2025-10-28 23:06:46,125 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 21000
2025-10-28 23:06:52,577 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 22000
2025-10-28 23:06:58,988 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 23000
2025-10-28 23:07:05,597 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 24000
2025-10-28 23:07:12,455 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 25000
2025-10-28 23:07:22,707 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 26000
2025-10-28 23:07:29,438 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 27000
2025-10-28 23:07:36,424 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 28000
2025-10-28 23:07:43,622 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 29000
2025-10-28 23:07:50,591 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) 30.000 of NewTodoCreated events created in 204285 ms
2025-10-28 23:07:52,251 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Fetch Performance
2025-10-28 23:07:52,251 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) min: 7 in ms
2025-10-28 23:07:52,251 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) P50: 20 in ms
2025-10-28 23:07:52,251 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) P90: 67 in ms
2025-10-28 23:07:52,252 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) P99: 1527 in ms
2025-10-28 23:07:52,252 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) max: 1604 in ms
2025-10-28 23:07:52,253 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) t_event - table_size '23 MB' - indexes_size '9112 kB' - total_size '32 MB'
2025-10-28 23:07:52,253 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) t_event - table_size '23 MB' - indexes_size '4336 kB' - total_size '28 MB'
 */
}
