package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.JdbcPostgresPassphraseRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionalException;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTest
class JdbcPostgresPassphraseRepositoryTest {

    @Inject
    Hasher hasher;

    @Inject
    DataSource dataSource;

    @Inject
    JdbcPostgresPassphraseRepository jdbcPostgresPassphraseRepository;

    @Inject
    TransactionManager transactionManager;

// Truncating is forbidden now
//    @BeforeEach
//    @AfterEach
//    void tearDown() {
//        try (final Connection connection = dataSource.getConnection();
//             final Statement stmt = connection.createStatement()) {
//            stmt.execute("TRUNCATE TABLE pulse.passphrase");
//        } catch (final SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }

    record PassphraseRecord(String ownedByHashed, String passphrase) {

        PassphraseRecord {
            Objects.requireNonNull(ownedByHashed);
            Objects.requireNonNull(passphrase);
        }
    }

    // store

    @Test
    @Order(1)
    void shouldFailToTruncateTable() {
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 TRUNCATE TABLE pulse.passphrase
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageStartingWith("ERROR: Truncating the passphrase table is forbidden.")
                .hasMessageContaining("PL/pgSQL function pulse.forbid_passphrase_truncate() line 3 at RAISE");
    }

    @Test
    @Order(2)
    void shouldStoreThrowTransactionalExceptionWhenNotExecutedInTransaction() {
        assertThatThrownBy(() -> jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1))
                .isExactlyInstanceOf(TransactionalException.class)
                .hasMessage("ARJUNA016110: Transaction is required for invocation");
    }

    @Test
    @Order(3)
    void shouldStorePassphrase() {
        // Given

        // When
        final Passphrase stored = QuarkusTransaction.requiringNew()
                .call(() -> jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1));

        // Then
        final List<PassphraseRecord> passphrases = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT owned_by_hashed AS owned_by_hashed, passphrase AS passphrase FROM pulse.passphrase
                             """);
             final ResultSet rs = ps.executeQuery()) {
            rs.next();
            passphrases.add(new PassphraseRecord(rs.getString("owned_by_hashed"), rs.getString("passphrase")));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        assertAll(
                () -> assertThat(stored).isEqualTo(PassphraseSample.PASSPHRASE_1),
                () -> assertThat(passphrases.size()).isEqualTo(1),
                () -> assertThat(passphrases.getFirst().ownedByHashed()).isEqualTo("825262468b4cb777358139eafbdec2e0477f898202d8cab60ae9c3a8e79a0de9"),
                () -> assertThat(passphrases.getFirst().passphrase()).startsWith("\\x")
        );
    }

    @Test
    @Order(4)
    void shouldThrowPassphraseAlreadyExistsExceptionWhenPassphraseAlreadyExists() {
        // Given
        final List<Integer> status = new ArrayList<>();

        // When && Then
        QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            QuarkusTransaction.joiningExisting().call(() -> {
                status.add(transactionManager.getStatus());
                assertThatThrownBy(() -> jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1))
                        .isExactlyInstanceOf(PassphraseAlreadyExistsException.class)
                        .hasFieldOrPropertyWithValue("ownedBy", Todo.OWNED_BY_USER_1);
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
    @Order(5)
    void shouldRollbackTransactionWhenUnableToStorePassphraseExceptionIsThrown() {
        // Given
        final List<Integer> counts = new ArrayList<>();
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();
        counts.add(count());

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_2, PassphraseSample.PASSPHRASE_1);
                        status.add(transactionManager.getStatus());

                        counts.add(count());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (SQLException e) {
                            // do nothing
                        }
                        jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_2, PassphraseSample.PASSPHRASE_1);
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
                        .hasCauseInstanceOf(UnableToStorePassphraseException.class),
                () -> assertThat(counts).containsExactly(1, 2, 1),
                () -> assertThat(status).containsExactly(Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_MARKED_ROLLBACK));
    }

    // update

    @Test
    @Order(6)
    void shouldUpdate() {
        // Given

        // When
        final Passphrase stored = QuarkusTransaction.requiringNew()
                .call(() -> jdbcPostgresPassphraseRepository.update(Todo.OWNED_BY_USER_1, new Passphrase(null)));

        // Then
        assertThat(stored).isEqualTo(new Passphrase(null));
    }

    @Test
    @Order(7)
    void shouldFailToUpdateOwnedBy() {
        // Given
        final String value = hasher.hash(Todo.OWNED_BY_USER_2).value();

        // When && Then
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE pulse.passphrase SET owned_by_hashed = ?
                                 """)) {
                ps.setString(1, value);
                try (final ResultSet rs = ps.executeQuery()) {
                }
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageStartingWith("ERROR: Modification of owned_by_hashed is forbidden.")
                .hasMessageContaining("PL/pgSQL function pulse.forbid_owned_by_hashed_update() line 4 at RAISE");
    }

    @Test
    @Order(8)
    void shouldFailToUpdatePassphraseWithANewOne() {
        // Given

        // When && Then
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE pulse.passphrase SET passphrase = ? WHERE owned_by_hashed = ?
                                 """)) {
                ps.setBytes(1, "boom".getBytes(StandardCharsets.UTF_8));
                ps.setString(2, "825262468b4cb777358139eafbdec2e0477f898202d8cab60ae9c3a8e79a0de9");
                try (final ResultSet rs = ps.executeQuery()) {
                }
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageStartingWith("ERROR: Modification of passphrase is forbidden.")
                .hasMessageContaining("PL/pgSQL function pulse.forbid_passphrase_update() line 5 at RAISE");
    }

    // findBy

    @Test
    @Order(9)
    void shouldFindByThrowTransactionalExceptionWhenNotExecutedInTransaction() {
        assertThatThrownBy(() -> jdbcPostgresPassphraseRepository.findBy(Todo.OWNED_BY_USER_1))
                .isExactlyInstanceOf(TransactionalException.class)
                .hasMessage("ARJUNA016110: Transaction is required for invocation");
    }

    @Test
    @Order(10)
    void shouldFindByReturnEmptyWhenPassphraseDoesNotExists() {
        // Given

        // When
        final Optional<Passphrase> passphrase = QuarkusTransaction.requiringNew()
                .call(() -> jdbcPostgresPassphraseRepository.findBy(Todo.OWNED_BY_USER_2));

        // Then
        assertThat(passphrase).isEmpty();
    }

    @Test
    @Order(11)
    void shouldFindByReturnStoredPassphrase() throws PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given
        QuarkusTransaction.requiringNew().call(() -> jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_2, PassphraseSample.PASSPHRASE_1));

        // When
        final Optional<Passphrase> passphrase = QuarkusTransaction.requiringNew().call(() -> jdbcPostgresPassphraseRepository.findBy(Todo.OWNED_BY_USER_2));

        // Then
        assertAll(
                () -> assertThat(passphrase).isNotEmpty(),
                () -> assertThat(passphrase.get().passphrase()).containsExactly(PassphraseSample.PASSPHRASE_1.passphrase()));
    }

    @Test
    @Order(12)
    void shouldRollbackTransactionWhenUnableToFindByPassphraseExceptionIsThrown() {
        // Given
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        jdbcPostgresPassphraseRepository.findBy(Todo.OWNED_BY_USER_1);
                        status.add(transactionManager.getStatus());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (SQLException e) {
                            // do nothing
                        }
                        jdbcPostgresPassphraseRepository.findBy(Todo.OWNED_BY_USER_1);
                        throw new IllegalStateException("Should not reach this point");
                    })
            ));
            status.add(transactionManager.getStatus());
            return null;
        }));

        // Then
        assertAll(
                () -> expectedException.get().isExactlyInstanceOf(QuarkusTransactionException.class)
                        .hasCauseInstanceOf(UnableToRetrievePassphraseException.class),
                () -> assertThat(status).containsExactly(Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_MARKED_ROLLBACK)
        );
    }

    // get

    @Test
    @Order(13)
    void shouldGetThrowTransactionalExceptionWhenNotExecutedInTransaction() {
        assertThatThrownBy(() -> jdbcPostgresPassphraseRepository.get(Todo.OWNED_BY_USER_1))
                .isExactlyInstanceOf(TransactionalException.class)
                .hasMessage("ARJUNA016110: Transaction is required for invocation");
    }

    @Test
    @Order(14)
    void shouldGetThrowsUnknownPassphraseExceptionWhenPassphraseDoesNotExists() {
        // Given

        // When && Then
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew()
                .call(() -> jdbcPostgresPassphraseRepository.get(Todo.OWNED_BY_USER_3)))
                .isExactlyInstanceOf(QuarkusTransactionException.class)
                .cause()
                .isExactlyInstanceOf(UnknownPassphraseException.class);
    }

    @Test
    @Order(15)
    void shouldGetReturnStoredPassphrase() {
        // Given

        // When
        final Passphrase passphrase = QuarkusTransaction.requiringNew().call(() -> jdbcPostgresPassphraseRepository.get(Todo.OWNED_BY_USER_2));

        // Then
        assertAll(
                () -> assertThat(passphrase).isNotNull(),
                () -> assertThat(passphrase.passphrase()).containsExactly(PassphraseSample.PASSPHRASE_1.passphrase()));
    }

    @Test
    @Order(16)
    void shouldRollbackTransactionWhenUnableToGetPassphraseExceptionIsThrown() {
        // Given
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        jdbcPostgresPassphraseRepository.get(Todo.OWNED_BY_USER_1);
                        status.add(transactionManager.getStatus());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (SQLException e) {
                            // do nothing
                        }
                        jdbcPostgresPassphraseRepository.get(Todo.OWNED_BY_USER_1);
                        throw new IllegalStateException("Should not reach this point");
                    })
            ));
            status.add(transactionManager.getStatus());
            return null;
        }));

        // Then
        assertAll(
                () -> expectedException.get().isExactlyInstanceOf(QuarkusTransactionException.class)
                        .hasCauseInstanceOf(UnableToRetrievePassphraseException.class),
                () -> assertThat(status).containsExactly(Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_MARKED_ROLLBACK)
        );
    }

    // list

    @Test
    @Order(17)
    void shouldListThrowTransactionalExceptionWhenNotExecutedInTransaction() {
        assertThatThrownBy(() -> jdbcPostgresPassphraseRepository.list(List.of(Todo.OWNED_BY_USER_5)))
                .isExactlyInstanceOf(TransactionalException.class)
                .hasMessage("ARJUNA016110: Transaction is required for invocation");
    }

    @Test
    @Order(18)
    void shouldListPassphrase() {
        // Given
        QuarkusTransaction.requiringNew().call(() -> {
            jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_5, PassphraseSample.PASSPHRASE_1);
            jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_6, PassphraseSample.PASSPHRASE_2);
            return null;
        });

        // When
        final List<RetrievedPassphrase> retrieved = QuarkusTransaction.joiningExisting().call(() -> jdbcPostgresPassphraseRepository.list(List.of(Todo.OWNED_BY_USER_5, Todo.OWNED_BY_USER_6, Todo.OWNED_BY_USER_7)));

        // Then
        assertThat(retrieved).containsExactlyInAnyOrder(new RetrievedPassphrase(Todo.OWNED_BY_USER_5, PassphraseSample.PASSPHRASE_1),
                new RetrievedPassphrase(Todo.OWNED_BY_USER_6, PassphraseSample.PASSPHRASE_2),
                new RetrievedPassphrase(Todo.OWNED_BY_USER_7, null));
    }

    @Test
    @Order(19)
    void shouldRollbackTransactionWhenUnableToListPassphraseExceptionIsThrown() {
        // Given
        final List<Integer> counts = new ArrayList<>();
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();
        counts.add(count());

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        jdbcPostgresPassphraseRepository.list(List.of(Todo.OWNED_BY_USER_5));
                        status.add(transactionManager.getStatus());

                        counts.add(count());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (SQLException e) {
                            // do nothing
                        }
                        jdbcPostgresPassphraseRepository.list(List.of(Todo.OWNED_BY_USER_5));
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
                        .hasCauseInstanceOf(UnableToRetrievePassphraseException.class),
                () -> assertThat(counts).containsExactly(4, 4, 4),
                () -> assertThat(status).containsExactly(Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_MARKED_ROLLBACK));
    }

    private int count() {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT COUNT(*) AS count FROM pulse.passphrase
                             """);
             final ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt("count");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
