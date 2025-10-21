package com.damdamdeo.pulse.extension.runtime.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.client.VaultClientException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class VaultPassphraseRepository implements PassphraseRepository {

    private static final String VAULT_PATH = "secret/owner";

    private final VaultKVSecretEngine vaultKVSecretEngine;

    public VaultPassphraseRepository(final VaultKVSecretEngine vaultKVSecretEngine) {
        this.vaultKVSecretEngine = Objects.requireNonNull(vaultKVSecretEngine);
    }

    @Override
    public Optional<char[]> retrieve(final OwnedBy ownedBy) {
        Objects.requireNonNull(ownedBy);
        final String path = VAULT_PATH + "/" + ownedBy.id();
        try {
            final Map<String, String> data = vaultKVSecretEngine.readSecret(path);
            if (data == null || !data.containsKey("passphrase")) {
                return Optional.empty();
            }
            final String passphrase = data.get("passphrase");
            return Optional.of(passphrase.toCharArray());
        } catch (final VaultClientException vaultClientException) {
            return Optional.empty();
        }
    }

    @Override
    public char[] store(final OwnedBy ownedBy, final char[] key) throws PassphraseAlreadyExistsException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(key);
        final String path = VAULT_PATH + "/" + ownedBy.id();
        final Map<String, String> secret = Map.of("passphrase", new String(key));
        try {
            final Map<String, String> existing = vaultKVSecretEngine.readSecret(path);
            if (existing != null && existing.containsKey("passphrase")) {
                throw new PassphraseAlreadyExistsException(ownedBy);
            } else {
                vaultKVSecretEngine.writeSecret(path, secret);
            }
        } catch (final VaultClientException vaultClientException) {
            vaultKVSecretEngine.writeSecret(path, secret);
        }
        return key.clone();
    }
}
