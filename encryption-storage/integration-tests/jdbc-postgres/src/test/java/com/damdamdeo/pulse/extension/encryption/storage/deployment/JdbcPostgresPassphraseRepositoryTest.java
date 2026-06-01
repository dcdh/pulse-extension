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
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    @BeforeEach
    @AfterEach
    void tearDown() {
        try (final Connection connection = dataSource.getConnection();
             final Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE passphrase");
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
    void shouldReturnEmptyWhenPassphraseDoesNotExists() throws UnableToRetrievePassphraseException {
        // Given

        // When
        final Optional<Passphrase> passphrase = jdbcPostgresPassphraseRepository.retrieve(Todo.OWNED_BY_USER_1);

        // Then
        assertThat(passphrase).isEmpty();
    }

    @Test
    void shouldReturnStoredPassphrase() throws UnableToRetrievePassphraseException, PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given
        jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE);

        // When
        final Optional<Passphrase> passphrase = jdbcPostgresPassphraseRepository.retrieve(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(passphrase).isNotEmpty(),
                () -> assertThat(passphrase.get().passphrase()).containsExactly(PassphraseSample.PASSPHRASE.passphrase()));
    }

    record PassphraseRecord(String ownedByHashed, String passphrase) {

        PassphraseRecord {
            Objects.requireNonNull(ownedByHashed);
            Objects.requireNonNull(passphrase);
        }
    }

    @Test
    void shouldStorePassphrase() throws PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given

        // When
        final Passphrase stored = jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE);

        // Then

        final List<PassphraseRecord> passphrases = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT owned_by_hashed AS owned_by_hashed, passphrase AS passphrase FROM passphrase
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
    void shouldThrowPassphraseAlreadyExistsExceptionWhenPassphraseAlreadyExists() throws PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given
        jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE);

        // When && Then
        assertThatThrownBy(() -> jdbcPostgresPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE))
                .isExactlyInstanceOf(PassphraseAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("ownedBy", Todo.OWNED_BY_USER_1);
    }
}
