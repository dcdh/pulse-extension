package com.damdamdeo.pulse.extension.traceability.deployment;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcPostgresInvolvedWithFullDetailsTraceRecorderRepositoryTest extends AbstractTraceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
//            .withApplicationRoot(javaArchive -> javaArchive.addClass(StubUsernameEncoder.class))
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Test
    void shouldStoreTraceRecorder() throws SQLException {
        // Given

        // Then
        final List<String> data = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement selectTraceabilityDetailsPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT trace_id, executed_at, source_value, execution_status, from_value FROM todo_taking.traceability_details
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
                             SELECT identifiable, executed_by_encoded_id, command_nb_of_times, command_unauthorized_nb_of_times, command_business_failed_nb_of_times, query_nb_of_times, query_unauthorized_nb_of_times, traceability_nb_of_times FROM todo_taking.traceability_identifiable
                             """);
             final PreparedStatement selectTraceabilityDetailsTraceabilityAggregatePreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT traceability_details_id, traceability_identifiable_identifiable, traceability_identifiable_executed_by_encoded_id FROM todo_taking.traceability_details_traceability_identifiable
                             """
             )) {
            ResultSet resultSet = selectTraceabilityDetailsPreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("trace_id"), resultSet.getTimestamp("executed_at").toInstant().toString(),
                        String.valueOf(resultSet.getInt("source_value")), String.valueOf(resultSet.getInt("execution_status")),
                        resultSet.getString("from_value")));
            }
            resultSet = selectExecutedByEncodedPreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("id"), resultSet.getString("executed_by_hashed"),
                        resultSet.getString("executed_by_encoded")));
            }
            resultSet = selectTraceabilityAggregatePreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("identifiable"),
                        resultSet.getString("executed_by_encoded_id"),
                        String.valueOf(resultSet.getLong("command_nb_of_times")),
                        String.valueOf(resultSet.getLong("command_unauthorized_nb_of_times")),
                        String.valueOf(resultSet.getLong("command_business_failed_nb_of_times")),
                        String.valueOf(resultSet.getLong("query_nb_of_times")),
                        String.valueOf(resultSet.getLong("query_unauthorized_nb_of_times")),
                        String.valueOf(resultSet.getLong("traceability_nb_of_times"))));
            }
            resultSet = selectTraceabilityDetailsTraceabilityAggregatePreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("traceability_details_id"),
                        resultSet.getString("traceability_identifiable_identifiable"),
                        resultSet.getString("traceability_identifiable_executed_by_encoded_id")));
            }
        }
        assertThat(data).containsExactlyInAnyOrder("1|2026-09-06T12:00:00Z|0|0|fromCommand",
                "2|2026-09-06T13:00:00Z|0|1|fromCommand",
                "3|2026-09-06T14:00:00Z|0|2|fromCommand",
                "4|2026-09-06T15:00:00Z|1|0|fromQuery",
                "5|2026-09-06T16:00:00Z|1|1|fromQuery",
                "6|2026-09-06T17:00:00Z|1|1|fromQuery",
                "7|2026-09-06T18:00:00Z|0|0|fromCommand",
                "8|2026-09-06T19:00:00Z|2|0|TRACEABILITY",
                "9|2026-09-06T20:00:00Z|2|0|TRACEABILITY",
                "1|EU:alice-hashed|EU:aliceEncoded",
                "4|EU:bob-hashed|EU:bobEncoded",
                "U000001-T000001|4|0|1|1|0|1|0",
                "U000001-T000001|1|3|0|0|0|0|0",
                "U000001-T000002|4|1|0|0|1|0|2",
                "U000001-T000001-CL000001|4|0|0|0|0|1|0",
                "3|U000001-T000001|4",
                "4|U000001-T000002|4",
                "5|U000001-T000001|4",
                "1|U000001-T000001|1",
                "7|U000001-T000002|4",
                "8|U000001-T000002|4",
                "6|U000001-T000001-CL000001|4",
                "2|U000001-T000001|4",
                "9|U000001-T000002|4");
    }
}
