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
import com.damdamdeo.pulse.extension.encryption.storage.runtime.vault.VaultPassphraseRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vault.VaultKVSecretEngine;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@QuarkusTest
class VaultPassphraseRepositoryTest {

    private static final String USER_1_SHA3_256 = "1db42019098571b7944ca44ddd7ecf3a93ccc58c35053906ba3bef5b45a5824d";

    private static final String SECRET_PATH = "todotaking_todo/owner/" + USER_1_SHA3_256;

    @Inject
    VaultKVSecretEngine vaultKVSecretEngine;

    @Inject
    VaultPassphraseRepository vaultPassphraseRepository;

    @Inject
    Hasher hasher;

    @AfterEach
    @BeforeEach
    void tearDown() {
        vaultKVSecretEngine.deleteSecret(SECRET_PATH);
    }

    @Test
    void shouldComputeUser1Hash() {
        // Given
        final OwnedBy original = Todo.OWNED_BY_USER_1;

        // When
        Hash<OwnedBy> hash = hasher.hash(original);

        // Then
        Assertions.assertThat(hash).isEqualTo(new Hash<OwnedBy>(USER_1_SHA3_256));
    }

    @Test
    void shouldReturnEmptyWhenPathDoesNotExists() throws UnableToRetrievePassphraseException {
        // Given

        // When
        final Optional<Passphrase> passphrase = vaultPassphraseRepository.retrieve(Todo.OWNED_BY_USER_1);

        // Then
        assertThat(passphrase).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenPassphraseDoesNotExists() throws UnableToRetrievePassphraseException {
        // Given
        vaultKVSecretEngine.writeSecret(SECRET_PATH, Map.of());

        // When
        final Optional<Passphrase> passphrase = vaultPassphraseRepository.retrieve(Todo.OWNED_BY_USER_1);

        // Then
        assertThat(passphrase).isEmpty();
    }

    @Test
    void shouldReturnStoredPassphrase() throws UnableToRetrievePassphraseException {
        // Given
        vaultKVSecretEngine.writeSecret(SECRET_PATH,
                Map.of("passphrase", new String(PassphraseSample.PASSPHRASE.passphrase())));

        // When
        final Optional<Passphrase> passphrase = vaultPassphraseRepository.retrieve(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(passphrase).isNotEmpty(),
                () -> Assertions.assertThat(passphrase.get().passphrase()).containsExactly(PassphraseSample.PASSPHRASE.passphrase()));
    }

    @Test
    void shouldStorePassphrase() throws PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given

        // When
        final Passphrase stored = vaultPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE);

        // Then
        final Map<String, String> secret = vaultKVSecretEngine.readSecret(SECRET_PATH);
        assertAll(
                () -> Assertions.assertThat(stored).isEqualTo(PassphraseSample.PASSPHRASE),
                () -> assertThat(secret).isEqualTo(Map.of("passphrase", new String(PassphraseSample.PASSPHRASE.passphrase())))
        );
    }

    @Test
    void shouldThrowPassphraseAlreadyExistsExceptionWhenPassphraseAlreadyExists() {
        // Given
        vaultKVSecretEngine.writeSecret(SECRET_PATH,
                Map.of("passphrase", new String(PassphraseSample.PASSPHRASE.passphrase())));

        // When && Then
        assertThatThrownBy(() -> vaultPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE))
                .isExactlyInstanceOf(PassphraseAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("ownedBy", Todo.OWNED_BY_USER_1);
    }
}
