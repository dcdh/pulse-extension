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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepositoryTest {

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
    void shouldStoreTraceRecorder() throws TraceRepositoryException, SQLException {
        // Given
        final TraceRecorder traceRecorder = new TraceRecorder(
                new TraceId(0L),
                new ExecutedAt(Instant.parse("2026-09-06T12:00:00Z")),
                new From("from"),
                List.of(
                        new EncodedTraceAggregateId(AnyAggregateId.from(TodoId.USER_1_TODO_1), new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                        new EncodedTraceAggregateId(AnyAggregateId.from(TodoId.USER_1_TODO_1), new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded")),
                        new EncodedTraceAggregateId(AnyAggregateId.from(TodoId.USER_1_TODO_1), new ExecutedByHashed("EU:bob-hashed"), new ExecutedByEncoded("EU:bobEncoded"))));

        // When
        jdbcPostgresInvolvedWithFullDetailsTraceRecorderRepository.store(traceRecorder);

        // Then
        final List<String> data = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement selectTraceabilityDetailsPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT trace_id, executed_at, from_value FROM todo_taking.traceability_details
                             """
             );
             final PreparedStatement selectExecutedByEncodedPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT id, executed_by_hashed, executed_by_encoded FROM todo_taking.executed_by_encoded
                             """
             );
             final PreparedStatement selectTraceabilityAggregatePreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT trace_id, aggregate_root_id, executed_by_encoded_id FROM todo_taking.traceability_aggregate
                             """)) {
            ResultSet resultSet = selectTraceabilityDetailsPreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("trace_id"), resultSet.getString("executed_at"),
                        resultSet.getString("from_value")));
            }
            resultSet = selectExecutedByEncodedPreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("id"), resultSet.getString("executed_by_hashed"),
                        resultSet.getString("executed_by_encoded")));
            }
            resultSet = selectTraceabilityAggregatePreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("trace_id"), resultSet.getString("aggregate_root_id"),
                        resultSet.getString("executed_by_encoded_id")));
            }
        }
        assertThat(data).containsExactly("0|2026-09-06 14:00:00+02|from",
                "1|EU:alice-hashed|EU:aliceEncoded",
                "3|EU:bob-hashed|EU:bobEncoded",
                "0|U000001-T000001|1",
                "0|U000001-T000001|3");
    }
}
