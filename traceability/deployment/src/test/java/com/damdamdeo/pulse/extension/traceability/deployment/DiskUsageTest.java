package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

class DiskUsageTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
//            .withApplicationRoot(javaArchive -> javaArchive.addClass(StubUsernameEncoder.class))
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Inject
    JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository jdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository;

    @Test
    void run() throws TraceRepositoryException {
        // Given
        for (long execution = 0; execution < 500; execution++) {
            final TraceRecorder traceRecorder = new TraceRecorder(
                    new TraceId(execution),
                    new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")),
                    new From("from"),
                    List.of(
                            new EncodedTraceAggregateId(AnyAggregateId.from(TodoId.USER_1_TODO_1), new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded"))));

            // When
            jdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository.store(traceRecorder);

            // Then
            final long finalExecution = execution;
            Stream.of("executed_by_encoded", "traceability_aggregate", "traceability_details", "traceability_details_traceability_aggregate")
                    .map(tableName -> "todo_taking." + tableName)
                    .forEach(schemaTableName -> {
                        try (final Connection connection = dataSource.getConnection();
                             final PreparedStatement preparedStatement = connection.prepareStatement(
                                     // language=sql
                                     """
                                             SELECT pg_size_pretty(pg_table_size('%1$s')) AS table_size,
                                                 pg_size_pretty(pg_indexes_size('%1$s')) AS indexes_size,
                                                 pg_size_pretty(pg_total_relation_size('%1$s')) AS total_size
                                             """.formatted(schemaTableName)
                             )) {
                            final ResultSet resultSet = preparedStatement.executeQuery();
                            resultSet.next();
                            System.out.printf(
                                    "%d - %s - table_size '%s' - indexes_size '%s' - total_size '%s'%n", finalExecution,
                                    schemaTableName,
                                    resultSet.getString("table_size"),
                                    resultSet.getString("indexes_size"),
                                    resultSet.getString("total_size"));
                        } catch (final SQLException exception) {
                            throw new RuntimeException(exception);
                        }
                    });
        }
    }
}
