package com.damdamdeo.pulse.extension.common.runtime.datasource;

import com.damdamdeo.pulse.extension.common.runtime.AbstractCommonTest;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifier;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepositoryException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.DuplicateConnectionIdentifierException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionalException;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class JdbcPostgresConnectionIdentifierRepositoryTest extends AbstractCommonTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql", Version.getVersion())
            ));

    @Inject
    JdbcPostgresConnectionIdentifierRepository jdbcPostgresConnectionIdentifierRepository;

    @Inject
    TransactionManager transactionManager;

    @Inject
    DataSource dataSource;

    @BeforeEach
    @AfterEach
    void tearDown() {
        try (final Connection connection = dataSource.getConnection();
             final Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE pulse.connection_identifier");
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
    void shouldStoreThrowTransactionalExceptionWhenNotExecutedInTransaction() {
        // Given
        final ConnectionIdentifier givenConnectionIdentifier = ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));

        // When && Then
        assertThatThrownBy(() -> jdbcPostgresConnectionIdentifierRepository.store(givenConnectionIdentifier, new UserAggregateId("U-000001")))
                .isExactlyInstanceOf(TransactionalException.class)
                .hasMessage("ARJUNA016110: Transaction is required for invocation");
    }

    @Test
    void shouldStore() {
        // Given
        final ConnectionIdentifier givenConnectionIdentifier = ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));

        // When
        QuarkusTransaction.requiringNew()
                .call(() -> jdbcPostgresConnectionIdentifierRepository.store(givenConnectionIdentifier, new UserAggregateId("U-000001")));

        // Then
        final List<DatabaseConnectionIdentifier> databaseConnectionIdentifiers = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT connection_identifier_hash AS connection_identifier_hash, identifiable_id AS identifiable_id FROM pulse.connection_identifier
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
    void shouldFailToStoreWhenConnectionIdentifierIsAlreadyStored() {
        // Given
        final List<Integer> status = new ArrayList<>();
        final ConnectionIdentifier givenConnectionIdentifier = ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));
        QuarkusTransaction.requiringNew().call(() -> jdbcPostgresConnectionIdentifierRepository.store(givenConnectionIdentifier, new UserAggregateId("U-000001")));

        // When && Then
        QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            QuarkusTransaction.joiningExisting().call(() -> {
                status.add(transactionManager.getStatus());
                assertThatThrownBy(() -> jdbcPostgresConnectionIdentifierRepository.store(givenConnectionIdentifier, new UserAggregateId("U-000002")))
                        .isInstanceOf(DuplicateConnectionIdentifierException.class)
                        .hasMessage("Connection identifier hash already exists: 0000000000000000000000000000000000000000000000000000000000000000.");
                status.add(transactionManager.getStatus());
                return null;
            });
            status.add(transactionManager.getStatus());
            return null;
        });
        // no rollback happened :)
        assertThat(status).containsExactly(Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
    }

    @Test
    void shouldRollbackTransactionWhenStoreTrowsConnectionIdentifierRepositoryException() {
        // Given
        final List<Integer> counts = new ArrayList<>();
        final List<Integer> status = new ArrayList<>();
        final ConnectionIdentifier givenConnectionIdentifier = ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));
        final UserAggregateId givenUserAggregateId = new UserAggregateId("U-000002");
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        jdbcPostgresConnectionIdentifierRepository.store(givenConnectionIdentifier, givenUserAggregateId);
                        status.add(transactionManager.getStatus());

                        counts.add(count());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (SQLException e) {
                            // do nothing
                        }
                        jdbcPostgresConnectionIdentifierRepository.store(givenConnectionIdentifier, givenUserAggregateId);
                        throw new IllegalStateException("Should not reach this point");
                    })
            ));
            status.add(transactionManager.getStatus());
            return null;
        }));

        counts.add(count());
        // Then
        // verify DB is empty (rollback happened)
        assertAll(
                () -> expectedException.get().isExactlyInstanceOf(QuarkusTransactionException.class)
                        .hasCauseInstanceOf(ConnectionIdentifierRepositoryException.class),
                () -> assertThat(counts).containsExactly(1, 0),
                () -> assertThat(status).containsExactly(Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_MARKED_ROLLBACK));
    }

    @Test
    void shouldFindByHashReturnFoundAggregateFromIdentifiable() throws ConnectionIdentifierRepositoryException {
        // Given
        final ConnectionIdentifier givenConnectionIdentifier = ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));
        QuarkusTransaction.requiringNew()
                .call(() -> jdbcPostgresConnectionIdentifierRepository.store(givenConnectionIdentifier, new UserAggregateId("U-000001")));

        // When
        final Optional<Identifiable> byHash = jdbcPostgresConnectionIdentifierRepository.find(givenConnectionIdentifier);

        // Then
        assertAll(
                () -> assertThat(byHash).isNotEmpty(),
                () -> assertThat(byHash.get().id()).isEqualTo("U-000001"));
    }

    @Test
    void shouldFIndByHashReturnEmptyWhenNoIdentifiableAssociated() throws ConnectionIdentifierRepositoryException {
        // Given
        final ConnectionIdentifier givenConnectionIdentifier = ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));

        // When
        final Optional<Identifiable> byHash = jdbcPostgresConnectionIdentifierRepository.find(givenConnectionIdentifier);

        // Then
        assertThat(byHash).isEqualTo(Optional.empty());
    }

    @Test
    void shouldRollbackTransactionWhenFindTrowsConnectionIdentifierRepositoryException() {
        // Given
        final ConnectionIdentifier givenConnectionIdentifier = ConnectionIdentifier.from(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"));
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        jdbcPostgresConnectionIdentifierRepository.find(givenConnectionIdentifier);
                        status.add(transactionManager.getStatus());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (SQLException e) {
                            // do nothing
                        }
                        jdbcPostgresConnectionIdentifierRepository.find(givenConnectionIdentifier);
                        throw new IllegalStateException("Should not reach this point");
                    }))
            );
            status.add(transactionManager.getStatus());
            return null;
        }));

        // Then
        assertAll(
                () -> expectedException.get().isExactlyInstanceOf(QuarkusTransactionException.class)
                        .hasCauseInstanceOf(ConnectionIdentifierRepositoryException.class),
                () -> assertThat(status).containsExactly(Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_MARKED_ROLLBACK)
        );
    }

    private int count() {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT COUNT(*) AS count FROM pulse.connection_identifier
                             """);
             final ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt("count");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
