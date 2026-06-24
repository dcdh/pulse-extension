package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.UnableToRetrievePassphraseException;
import com.damdamdeo.pulse.extension.core.encryption.UnableToStorePassphraseException;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.vault.JdbcPostgresPassphraseRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionalException;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

@QuarkusTest
class JdbcPostgresPassphraseRepositoryTest {

    private static final String USER_1_SHA3_256 = "1db42019098571b7944ca44ddd7ecf3a93ccc58c35053906ba3bef5b45a5824d";

    @Inject
    Hasher hasher;

    @Inject
    DataSource dataSource;

    @Inject
    JdbcPostgresPassphraseRepository jdbcPostgresPassphraseRepository;

    @Inject
    TransactionManager transactionManager;

    @BeforeEach
    @AfterEach
    void tearDown() {
        try (final Connection connection = dataSource.getConnection();
             final Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE pulse.passphrase");
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldComputeUser1Hash() {
        // Given
        final OwnedBy original = Todo.OWNED_BY_USER_1;

        // When
        Hash<OwnedBy> hash = hasher.hash(original);

        // Then
        assertThat(hash).isEqualTo(new Hash<OwnedBy>(USER_1_SHA3_256));
    }

    @Test
    void shouldRetrieveThrowTransactionalExceptionWhenNotExecutedInTransaction() {
        assertThatThrownBy(() -> jdbcPostgresPassphraseRepository.retrieve(Todo.OWNED_BY_USER_1))
                .isExactlyInstanceOf(TransactionalException.class)
                .hasMessage("ARJUNA016110: Transaction is required for invocation");
    }

    @Test
    void shouldReturnEmptyWhenPassphraseDoesNotExists() {
        // Given

        // When
        final Optional<Passphrase> passphrase = QuarkusTransaction.requiringNew()
                .call(() -> jdbcPostgresPassphraseRepository.retrieve(Todo.OWNED_BY_USER_1));

        // Then
        assertThat(passphrase).isEmpty();
    }

    @Test
    void shouldReturnStoredPassphrase() {
        // Given
        QuarkusTransaction.requiringNew().call(() -> jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE));

        // When
        final Optional<Passphrase> passphrase = QuarkusTransaction.requiringNew().call(() -> jdbcPostgresPassphraseRepository.retrieve(Todo.OWNED_BY_USER_1));

        // Then
        assertAll(
                () -> assertThat(passphrase).isNotEmpty(),
                () -> assertThat(passphrase.get().passphrase()).containsExactly(PassphraseSample.PASSPHRASE.passphrase()));
    }

    @Test
    void shouldRollbackTransactionWhenUnableToRetrievePassphraseExceptionIsThrown() {
        // Given
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        jdbcPostgresPassphraseRepository.retrieve(Todo.OWNED_BY_USER_1);
                        status.add(transactionManager.getStatus());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (SQLException e) {
                            // do nothing
                        }
                        jdbcPostgresPassphraseRepository.retrieve(Todo.OWNED_BY_USER_1);
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

    record PassphraseRecord(String ownedByHashed, String passphrase) {

        PassphraseRecord {
            Objects.requireNonNull(ownedByHashed);
            Objects.requireNonNull(passphrase);
        }
    }

    @Test
    void shouldStoreThrowTransactionalExceptionWhenNotExecutedInTransaction() {
        assertThatThrownBy(() -> jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE))
                .isExactlyInstanceOf(TransactionalException.class)
                .hasMessage("ARJUNA016110: Transaction is required for invocation");
    }

    @Test
    void shouldStorePassphrase() {
        // Given

        // When
        final Passphrase stored = QuarkusTransaction.requiringNew()
                .call(() -> jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE));

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
                () -> assertThat(stored).isEqualTo(PassphraseSample.PASSPHRASE),
                () -> assertThat(passphrases.size()).isEqualTo(1),
                () -> assertThat(passphrases.getFirst().ownedByHashed()).isEqualTo("1db42019098571b7944ca44ddd7ecf3a93ccc58c35053906ba3bef5b45a5824d"),
                () -> assertThat(passphrases.getFirst().passphrase()).startsWith("\\x")
        );
    }

    @Test
    void shouldThrowPassphraseAlreadyExistsExceptionWhenPassphraseAlreadyExists() {
        // Given
        final List<Integer> status = new ArrayList<>();
        QuarkusTransaction.requiringNew().call(() -> jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE));

        // When && Then
        QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            QuarkusTransaction.joiningExisting().call(() -> {
                status.add(transactionManager.getStatus());
                assertThatThrownBy(() -> jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE))
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
    void shouldRollbackTransactionWhenUnableToStorePassphraseExceptionIsThrown() {
        // Given
        final List<Integer> counts = new ArrayList<>();
        final List<Integer> status = new ArrayList<>();
        final AtomicReference<AbstractThrowableAssert<?, ?>> expectedException = new AtomicReference<>();

        // When
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().call(() -> {
            status.add(transactionManager.getStatus());
            expectedException.set(assertThatThrownBy(() ->
                    QuarkusTransaction.joiningExisting().call(() -> {
                        jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE);
                        status.add(transactionManager.getStatus());

                        counts.add(count());

                        try (final Connection c = dataSource.getConnection()) {
                            c.createStatement().execute("SELECT pg_terminate_backend(pg_backend_pid())");
                        } catch (SQLException e) {
                            // do nothing
                        }
                        jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE);
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
                () -> assertThat(counts).containsExactly(1, 0),
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
