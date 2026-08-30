package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.ExecutedByEncodedRepositoryException;
import com.damdamdeo.pulse.extension.traceability.runtime.JdbcPostgresExecutedByEncodedRepository;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcPostgresExecutedByEncodedRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresExecutedByEncodedRepository jdbcPostgresExecutedByEncodedRepository;

    @Inject
    DataSource dataSource;

    @Order(1)
    @Test
    void shouldStore() throws ExecutedByEncodedRepositoryException, SQLException {
        // Given

        // When
        jdbcPostgresExecutedByEncodedRepository.store(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded"));

        // Then
        final List<String> data = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement selectTraceabilityDetailsPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT id, executed_by_hashed, executed_by_encoded FROM todo_taking.executed_by_encoded
                             """
             )) {
            final ResultSet resultSet = selectTraceabilityDetailsPreparedStatement.executeQuery();
            while (resultSet.next()) {
                data.add(String.join("|", resultSet.getString("id"), resultSet.getString("executed_by_hashed"),
                        resultSet.getString("executed_by_encoded")));
            }
        }
        assertThat(data).containsExactly("1|EU:alice-hashed|EU:aliceEncoded");
    }

    @Order(2)
    @Test
    void shouldNotStoreWhenAlreadyPresent() throws ExecutedByEncodedRepositoryException, SQLException {
        // Given

        // When
        jdbcPostgresExecutedByEncodedRepository.store(new ExecutedByHashed("EU:alice-hashed"), new ExecutedByEncoded("EU:aliceEncoded"));

        // Then
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement selectTraceabilityDetailsPreparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT count(*) AS count FROM todo_taking.executed_by_encoded
                             """
             )) {
            final ResultSet resultSet = selectTraceabilityDetailsPreparedStatement.executeQuery();
            resultSet.next();
            assertThat(resultSet.getLong("count")).isEqualTo(1L);
        }
    }

    @Order(3)
    @Test
    void shouldFindByReturnWhenPresent() throws ExecutedByEncodedRepositoryException {
        // Given

        // When
        final ExecutedByEncoded by = jdbcPostgresExecutedByEncodedRepository.findBy(new ExecutedByHashed("EU:alice-hashed"));

        // Then
        assertThat(by).isEqualTo(new ExecutedByEncoded("EU:aliceEncoded"));
    }

    @Order(4)
    @Test
    void shouldFindByReturnNullWhenNotPresent() throws ExecutedByEncodedRepositoryException {
        // Given

        // When
        final ExecutedByEncoded by = jdbcPostgresExecutedByEncodedRepository.findBy(new ExecutedByHashed("EU:bob-hashed"));

        // Then
        assertThat(by).isNull();
    }
}
