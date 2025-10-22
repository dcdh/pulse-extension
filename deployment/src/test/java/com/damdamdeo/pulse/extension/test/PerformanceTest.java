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
import jakarta.inject.Inject;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

class PerformanceTest {

    static final Logger LOGGER = Logger.getLogger(PerformanceTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .withConfigurationResource("application.properties");

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
        watch.start();
        final Optional<TodoProjection> foundBy = todoProjectionProjectionFromEventStore.findBy(new OwnedBy("Performance"), new TodoId("Performance", 1_000L), new TodoProjectionSingleResultAggregateQuery());
        watch.stop();
        LOGGER.info("Fetch Performance/1_000L in %d ms - '%s'".formatted(watch.getTime(TimeUnit.MILLISECONDS), foundBy.get()));
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
2025-10-22 21:52:56,955 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Start execute performance test
2025-10-22 21:52:56,955 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Create 30.000 of NewTodoCreated events
2025-10-22 21:52:56,957 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 0
2025-10-22 21:53:02,826 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 1000
2025-10-22 21:53:08,034 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 2000
2025-10-22 21:53:13,185 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 3000
2025-10-22 21:53:18,262 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 4000
2025-10-22 21:53:23,320 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 5000
2025-10-22 21:53:28,314 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 6000
2025-10-22 21:53:33,408 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 7000
2025-10-22 21:53:38,398 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 8000
2025-10-22 21:53:43,425 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 9000
2025-10-22 21:53:48,438 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 10000
2025-10-22 21:53:53,465 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 11000
2025-10-22 21:53:59,327 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 12000
2025-10-22 21:54:08,530 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 13000
2025-10-22 21:54:17,950 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 14000
2025-10-22 21:54:27,488 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 15000
2025-10-22 21:54:37,338 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 16000
2025-10-22 21:54:46,969 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 17000
2025-10-22 21:54:56,636 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 18000
2025-10-22 21:55:06,030 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 19000
2025-10-22 21:55:15,741 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 20000
2025-10-22 21:55:25,378 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 21000
2025-10-22 21:55:34,862 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 22000
2025-10-22 21:55:44,383 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 23000
2025-10-22 21:55:54,072 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 24000
2025-10-22 21:56:01,648 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 25000
2025-10-22 21:56:10,775 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 26000
2025-10-22 21:56:20,393 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 27000
2025-10-22 21:56:29,979 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 28000
2025-10-22 21:56:39,649 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Current creation 29000
2025-10-22 21:56:49,349 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) 30.000 of NewTodoCreated events created in 232391 ms
2025-10-22 21:56:49,349 FINE  [com.dam.pul.ext.run.pro.JdbcProjectionFromEventStore] (main) WITH decrypted AS (
  SELECT
    aggregate_root_id,
    aggregate_root_type,
    pgp_sym_decrypt(aggregate_root_payload, 'j8XPX7qt2G%7wwOv6-pu!Iz6M5N9e_Lb')::jsonb AS decrypted_aggregate_root_payload,
    in_relation_with
  FROM t_aggregate_root
  WHERE in_relation_with = 'Performance/1000'
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
  AND d.aggregate_root_id = 'Performance/1000'
GROUP BY d.aggregate_root_id, d.aggregate_root_type, d.decrypted_aggregate_root_payload, d.in_relation_with;

2025-10-22 21:56:49,375 FINE  [com.dam.pul.ext.run.pro.JdbcProjectionFromEventStore] (main) {"status": "IN_PROGRESS", "todoId": {"user": "Performance", "sequence": 1000}, "checklist": [], "important": "false", "description": "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."}
2025-10-22 21:56:49,387 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) Fetch Performance/1_000L in 33 ms - 'TodoProjection[todoId=TodoId[user=Performance, sequence=1000], description=Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum., status=IN_PROGRESS, important=false]'
2025-10-22 21:56:49,390 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) t_event - table_size '23 MB' - indexes_size '9112 kB' - total_size '32 MB'
2025-10-22 21:56:49,390 INFO  [com.dam.pul.ext.tes.PerformanceTest] (main) t_event - table_size '23 MB' - indexes_size '4336 kB' - total_size '28 MB'
 */
}
