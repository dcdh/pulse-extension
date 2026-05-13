package com.damdamdeo.pulse.extension.common.runtime.vault;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vault.VaultKVSecretEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class VaultPassphraseRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    private static final String DAMIEN_SHA3_256 = "df20d988f004d3c742aef4bb86c7ac4735e93c0c81596f5807b641ea0ce1179c";

    private static final String SECRET_PATH = "secret/owner/" + DAMIEN_SHA3_256;

    @Inject
    VaultKVSecretEngine vaultKVSecretEngine;

    @Inject
    VaultPassphraseRepository vaultPassphraseRepository;

    @Inject
    Hasher hasher;

    @BeforeEach
    void setup() {
        vaultKVSecretEngine.deleteSecret(SECRET_PATH);
    }

    @Test
    void shouldComputeDamienHash() {
        // Given
        final String original = "Damien";

        // When
        Hash hash = hasher.hash(original);

        // Then
        assertThat(hash).isEqualTo(new Hash(DAMIEN_SHA3_256));
    }

    @Test
    void shouldReturnEmptyWhenPathDoesNotExists() {
        // Given

        // When
        final Optional<Passphrase> passphrase = vaultPassphraseRepository.retrieve(new OwnedBy("Damien"));

        // Then
        assertThat(passphrase).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenPassphraseDoesNotExists() {
        // Given
        vaultKVSecretEngine.writeSecret(SECRET_PATH, Map.of());

        // When
        final Optional<Passphrase> passphrase = vaultPassphraseRepository.retrieve(new OwnedBy("Damien"));

        // Then
        assertThat(passphrase).isEmpty();
    }

    @Test
    void shouldReturnStoredPassphrase() {
        // Given
        vaultKVSecretEngine.writeSecret(SECRET_PATH,
                Map.of("passphrase", new String(PassphraseSample.PASSPHRASE.passphrase())));

        // When
        final Optional<Passphrase> passphrase = vaultPassphraseRepository.retrieve(new OwnedBy("Damien"));

        // Then
        assertAll(
                () -> assertThat(passphrase).isNotEmpty(),
                () -> assertThat(passphrase.get().passphrase()).containsExactly(PassphraseSample.PASSPHRASE.passphrase()));
    }

    @Test
    void shouldStorePassphrase() {
        // Given

        // When
        final Passphrase stored = vaultPassphraseRepository.store(new OwnedBy("Damien"), PassphraseSample.PASSPHRASE);

        // Then
        final Map<String, String> secret = vaultKVSecretEngine.readSecret(SECRET_PATH);
        assertAll(
                () -> assertThat(stored).isEqualTo(PassphraseSample.PASSPHRASE),
                () -> assertThat(secret).isEqualTo(Map.of("passphrase", new String(PassphraseSample.PASSPHRASE.passphrase())))
        );
    }
}
