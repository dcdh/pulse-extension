package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresInvolvedTraceRecorderRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcPostgresInvolvedTraceRecorderRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED")
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Inject
    JdbcPostgresInvolvedTraceRecorderRepository jdbcPostgresInvolvedTraceRecorderRepository;

    @Test
    void shouldStoreTraceRecorder() throws TraceRepositoryException, SQLException {
        // Given
        final TraceRecorder traceRecorder = new TraceRecorder(
                new TraceId(0L),
                new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")),
                Source.COMMAND,
                new From("from"),
                List.of(
                        new EncodedTraceAggregateId(AnyAggregateId.from(TodoId.USER_1_TODO_1), new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                        new EncodedTraceAggregateId(AnyAggregateId.from(TodoId.USER_1_TODO_1), new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                        new EncodedTraceAggregateId(AnyAggregateId.from(TodoId.USER_1_TODO_1), new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded"))));

        // When
        jdbcPostgresInvolvedTraceRecorderRepository.store(traceRecorder);

        // Then
        final List<String> data = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement selectExecutedByEncodedPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT id, executed_by_hashed, executed_by_encoded FROM todo_taking.executed_by_encoded
                             """
             );
             final PreparedStatement selectTraceabilityAggregatePreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT aggregate_root_id, executed_by_encoded_id, command_nb_of_times, query_nb_of_times FROM todo_taking.traceability_aggregate
                             """)) {
            ResultSet resultSet = selectExecutedByEncodedPreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("id"), resultSet.getString("executed_by_hashed"), resultSet.getString("executed_by_encoded")));
            }
            resultSet = selectTraceabilityAggregatePreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("aggregate_root_id"), resultSet.getString("executed_by_encoded_id"),
                        String.valueOf(resultSet.getLong("command_nb_of_times")),
                        String.valueOf(resultSet.getLong("query_nb_of_times"))));
            }
        }
        assertThat(data).containsExactly("1|EU:alice-hashed|EU:aliceEncoded",
                "3|EU:bob-hashed|EU:bobEncoded",
                "U000001-T000001|1|2|0",
                "U000001-T000001|3|1|0");
    }
}
