package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.VaultPassphraseRepository;
import com.damdamdeo.pulse.extension.hasher.runtime.HasherProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vault.VaultKVSecretEngine;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@QuarkusTest
class VaultPassphraseRepositoryTest {

    private static final String SECRET_PATH_OWNED_BY_USER_1 = "owner/" + HasherProvider.GIVEN_OWNED_BY_EXPECTED_HASH.get(Todo.OWNED_BY_USER_1).value();
    private static final String SECRET_PATH_OWNED_BY_USER_2 = "owner/" + HasherProvider.GIVEN_OWNED_BY_EXPECTED_HASH.get(Todo.OWNED_BY_USER_2).value();

    @Inject
    VaultKVSecretEngine vaultKVSecretEngine;

    @Inject
    VaultPassphraseRepository vaultPassphraseRepository;

    @Inject
    Hasher hasher;

    @AfterEach
    @BeforeEach
    void tearDown() {
        vaultKVSecretEngine.deleteSecret(SECRET_PATH_OWNED_BY_USER_1);
    }

    @Test
    void shouldFindByReturnEmptyWhenPathDoesNotExists() throws UnableToRetrievePassphraseException {
        // Given

        // When
        final Optional<Passphrase> passphrase = vaultPassphraseRepository.findBy(Todo.OWNED_BY_USER_1);

        // Then
        assertThat(passphrase).isEmpty();
    }

    @Test
    void shouldFindByReturnEmptyWhenPassphraseDoesNotExists() throws UnableToRetrievePassphraseException {
        // Given
        vaultKVSecretEngine.writeSecret(SECRET_PATH_OWNED_BY_USER_1, Map.of());

        // When
        final Optional<Passphrase> passphrase = vaultPassphraseRepository.findBy(Todo.OWNED_BY_USER_1);

        // Then
        assertThat(passphrase).isEmpty();
    }

    @Test
    void shouldFindByReturnStoredPassphrase() throws UnableToRetrievePassphraseException {
        // Given
        vaultKVSecretEngine.writeSecret(SECRET_PATH_OWNED_BY_USER_1,
                Map.of("passphrase", new String(PassphraseSample.PASSPHRASE_1.passphrase())));

        // When
        final Optional<Passphrase> passphrase = vaultPassphraseRepository.findBy(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(passphrase).isNotEmpty(),
                () -> Assertions.assertThat(passphrase.get().passphrase()).containsExactly(PassphraseSample.PASSPHRASE_1.passphrase()));
    }

    @Test
    void shouldStorePassphrase() throws PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given

        // When
        final Passphrase stored = vaultPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1);

        // Then
        final Map<String, String> secret = vaultKVSecretEngine.readSecret(SECRET_PATH_OWNED_BY_USER_1);
        assertAll(
                () -> Assertions.assertThat(stored).isEqualTo(PassphraseSample.PASSPHRASE_1),
                () -> assertThat(secret).isEqualTo(Map.of("passphrase", new String(PassphraseSample.PASSPHRASE_1.passphrase())))
        );
    }

    @Test
    void shouldStoreThrowPassphraseAlreadyExistsExceptionWhenPassphraseAlreadyExists() {
        // Given
        vaultKVSecretEngine.writeSecret(SECRET_PATH_OWNED_BY_USER_1,
                Map.of("passphrase", new String(PassphraseSample.PASSPHRASE_1.passphrase())));

        // When && Then
        assertThatThrownBy(() -> vaultPassphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1))
                .isExactlyInstanceOf(PassphraseAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("ownedBy", Todo.OWNED_BY_USER_1);
    }

    @Test
    void shouldUpdate() throws UnableToStorePassphraseException, UnknownPassphraseException {
        // Given
        vaultKVSecretEngine.writeSecret(SECRET_PATH_OWNED_BY_USER_1,
                Map.of("passphrase", new String(PassphraseSample.PASSPHRASE_1.passphrase())));

        // When
        final Passphrase passphrase = vaultPassphraseRepository.update(Todo.OWNED_BY_USER_1, new Passphrase(null));

        // Then
        assertThat(passphrase).isEqualTo(new Passphrase(null));
    }

    @Test
    void shouldUpdateThrowUnknownPassphraseExceptionWhenPassphraseDoesNotExists() {
        // Given

        // When && Then
        assertThatThrownBy(() -> vaultPassphraseRepository.update(Todo.OWNED_BY_USER_1, new Passphrase(null)))
                .isExactlyInstanceOf(UnknownPassphraseException.class)
                .hasFieldOrPropertyWithValue("ownedBy", Todo.OWNED_BY_USER_1);
    }

    @Test
    void shouldListPassphrase() throws UnableToRetrievePassphraseException {
        // Given
        vaultKVSecretEngine.writeSecret(SECRET_PATH_OWNED_BY_USER_1,
                Map.of("passphrase", new String(PassphraseSample.PASSPHRASE_1.passphrase())));
        vaultKVSecretEngine.writeSecret(SECRET_PATH_OWNED_BY_USER_2,
                Map.of("passphrase", new String(PassphraseSample.PASSPHRASE_2.passphrase())));

        // When
        final List<RetrievedPassphrase> retrieved = vaultPassphraseRepository.list(List.of(Todo.OWNED_BY_USER_1, Todo.OWNED_BY_USER_2, Todo.OWNED_BY_USER_3));

        // Then
        assertThat(retrieved).containsExactlyInAnyOrder(new RetrievedPassphrase(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1),
                new RetrievedPassphrase(Todo.OWNED_BY_USER_2, PassphraseSample.PASSPHRASE_2),
                new RetrievedPassphrase(Todo.OWNED_BY_USER_3, null));
    }
}
