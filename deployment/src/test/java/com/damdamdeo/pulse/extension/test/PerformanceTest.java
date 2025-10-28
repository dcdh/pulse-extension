package com.damdamdeo.pulse.extension.test;

import com.damdamdeo.pulse.extension.core.*;
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
import java.util.LongSummaryStatistics;
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
        public String query(final char[] passphrase, final AggregateId aggregateId) {
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
                    """.formatted(new String(passphrase), aggregateId.id());
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
        final LongSummaryStatistics stats = LongStream.range(0, executionsIn.size())
                .map(i -> executionsIn.get((int) i))
                .summaryStatistics();
        LOGGER.info("Fetch Performance");
        LOGGER.info("count: %d".formatted(stats.getCount()));
        LOGGER.info("sum: %d in ms".formatted(stats.getSum()));
        LOGGER.info("min: %d in ms".formatted(stats.getMin()));
        LOGGER.info("average: %f in ms".formatted(stats.getAverage()));
        LOGGER.info("max: %d in ms".formatted(stats.getMax()));

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
2025-10-28 22:51:37,414 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Start execute performance test
2025-10-28 22:51:37,414 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Create 30.000 of NewTodoCreated events
2025-10-28 22:51:37,415 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 0
2025-10-28 22:51:44,860 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 1000
2025-10-28 22:51:52,086 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 2000
2025-10-28 22:51:59,001 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 3000
2025-10-28 22:52:08,959 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 4000
2025-10-28 22:52:16,547 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 5000
2025-10-28 22:52:23,634 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 6000
2025-10-28 22:52:30,073 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 7000
2025-10-28 22:52:37,582 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 8000
2025-10-28 22:52:47,372 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 9000
2025-10-28 22:52:54,892 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 10000
2025-10-28 22:53:02,011 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 11000
2025-10-28 22:53:09,243 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 12000
2025-10-28 22:53:17,500 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 13000
2025-10-28 22:53:27,430 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 14000
2025-10-28 22:53:35,360 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 15000
2025-10-28 22:53:42,421 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 16000
2025-10-28 22:53:49,957 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 17000
2025-10-28 22:53:56,915 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 18000
2025-10-28 22:54:06,285 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 19000
2025-10-28 22:54:12,121 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 20000
2025-10-28 22:54:18,360 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 21000
2025-10-28 22:54:23,903 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 22000
2025-10-28 22:54:31,395 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 23000
2025-10-28 22:54:39,650 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 24000
2025-10-28 22:54:46,849 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 25000
2025-10-28 22:54:53,163 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 26000
2025-10-28 22:55:00,120 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 27000
2025-10-28 22:55:08,691 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 28000
2025-10-28 22:55:14,825 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 29000
2025-10-28 22:55:21,820 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) 30.000 of NewTodoCreated events created in 224405 ms
2025-10-28 22:55:24,053 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Fetch Performance
2025-10-28 22:55:24,054 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) count: 1000
2025-10-28 22:55:24,054 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) sum: 162534 in ms
2025-10-28 22:55:24,054 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) min: 6 in ms
2025-10-28 22:55:24,054 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) average: 162,534000 in ms
2025-10-28 22:55:24,054 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) max: 2184 in ms
2025-10-28 22:55:24,056 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) t_event - table_size '23 MB' - indexes_size '9112 kB' - total_size '32 MB'
2025-10-28 22:55:24,056 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) t_event - table_size '23 MB' - indexes_size '4336 kB' - total_size '28 MB'
 */
}
