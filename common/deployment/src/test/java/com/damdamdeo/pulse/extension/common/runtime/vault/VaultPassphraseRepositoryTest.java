package com.damdamdeo.pulse.extension.common.runtime.vault;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
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

    private static final String SECRET_PATH = "secret/owner/Damien";

    @Inject
    VaultKVSecretEngine vaultKVSecretEngine;

    @Inject
    VaultPassphraseRepository vaultPassphraseRepository;

    @BeforeEach
    void setup() {
        vaultKVSecretEngine.deleteSecret(SECRET_PATH);
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
