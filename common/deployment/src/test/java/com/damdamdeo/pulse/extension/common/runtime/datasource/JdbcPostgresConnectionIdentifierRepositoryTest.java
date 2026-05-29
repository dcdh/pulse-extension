package com.damdamdeo.pulse.extension.common.runtime.datasource;

import com.damdamdeo.pulse.extension.common.runtime.AbstractCommonTest;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifier;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepositoryException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.DuplicateConnectionIdentifierException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class JdbcPostgresConnectionIdentifierRepositoryTest extends AbstractCommonTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "true")
            .overrideRuntimeConfigKey("pulse.datasource.init-at-startup", "true")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresConnectionIdentifierRepository jdbcPostgresConnectionIdentifierRepository;

    @Inject
    DataSource dataSource;

    @BeforeEach
    @AfterEach
    void tearDown() {
        try (final Connection connection = dataSource.getConnection();
             final Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE connection_identifier");
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    record UserAggregateId(String id) implements AggregateId {

        UserAggregateId {
            Objects.requireNonNull(id);
        }
    }

    record DatabaseConnectionIdentifier(String connectionIdentifierHash, String identifiableId) {

        DatabaseConnectionIdentifier {
            Objects.requireNonNull(connectionIdentifierHash);
            Objects.requireNonNull(identifiableId);
        }
    }

    @Test
    void shouldStore() throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
        // Given
        final Hash<ConnectionIdentifier> givenConnectionIdentifier = new Hash<>("0000000000000000000000000000000000000000000000000000000000000000");

        // When
        jdbcPostgresConnectionIdentifierRepository.store(givenConnectionIdentifier, new UserAggregateId("U-000001"));

        // Then
        final List<DatabaseConnectionIdentifier> databaseConnectionIdentifiers = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT connection_identifier_hash AS connection_identifier_hash, identifiable_id AS identifiable_id FROM connection_identifier
                             """);
             final ResultSet rs = ps.executeQuery()) {
            rs.next();
            databaseConnectionIdentifiers.add(new DatabaseConnectionIdentifier(rs.getString("connection_identifier_hash"), rs.getString("identifiable_id")));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        assertThat(databaseConnectionIdentifiers).containsExactly(
                new DatabaseConnectionIdentifier("0000000000000000000000000000000000000000000000000000000000000000", "U-000001"));
    }

    @Test
    void shouldFailToStoreWhenConnectionIdentifierIsAlreadyStored() throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
        // Given
        final Hash<ConnectionIdentifier> givenConnectionIdentifier = new Hash<>("0000000000000000000000000000000000000000000000000000000000000000");
        jdbcPostgresConnectionIdentifierRepository.store(givenConnectionIdentifier, new UserAggregateId("U-000001"));

        // When && Then
        assertThatThrownBy(() -> jdbcPostgresConnectionIdentifierRepository.store(givenConnectionIdentifier, new UserAggregateId("U-000002")))
                .isInstanceOf(DuplicateConnectionIdentifierException.class)
                .hasMessage("Connection identifier hash already exists: 0000000000000000000000000000000000000000000000000000000000000000.")
                .hasRootCauseInstanceOf(PSQLException.class);
    }

    @Test
    void shouldFindByHashReturnFoundAggregateFromIdentifiable() throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
        // Given
        final Hash<ConnectionIdentifier> givenConnectionIdentifier = new Hash<>("0000000000000000000000000000000000000000000000000000000000000000");
        jdbcPostgresConnectionIdentifierRepository.store(givenConnectionIdentifier, new UserAggregateId("U-000001"));

        // When
        final Optional<Identifiable> byHash = jdbcPostgresConnectionIdentifierRepository.findByHash(givenConnectionIdentifier);

        // Then
        assertAll(
                () -> assertThat(byHash).isNotEmpty(),
                () -> assertThat(byHash.get().id()).isEqualTo("U-000001"));
    }

    @Test
    void shouldFIndByHashReturnEmptyWhenNoIdentifiableAssociated() throws ConnectionIdentifierRepositoryException {
        // Given
        final Hash<ConnectionIdentifier> givenConnectionIdentifier = new Hash<>("0000000000000000000000000000000000000000000000000000000000000000");

        // When
        final Optional<Identifiable> byHash = jdbcPostgresConnectionIdentifierRepository.findByHash(givenConnectionIdentifier);

        // Then
        assertThat(byHash).isEqualTo(Optional.empty());
    }
}
