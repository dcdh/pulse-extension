package com.damdamdeo.pulse.extension.encryption.storage.runtime.vault;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.client.VaultClientException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class VaultPassphraseRepository implements PassphraseRepository {

    private final VaultKVSecretEngine vaultKVSecretEngine;
    private final Hasher hasher;

    public VaultPassphraseRepository(final VaultKVSecretEngine vaultKVSecretEngine,
                                     final Hasher hasher) {
        this.vaultKVSecretEngine = Objects.requireNonNull(vaultKVSecretEngine);
        this.hasher = Objects.requireNonNull(hasher);
    }

    @Override
    public Optional<Passphrase> retrieve(final OwnedBy ownedBy) throws UnableToRetrievePassphraseException {
        Objects.requireNonNull(ownedBy);
        final String path = getOwnedByHasherStringBiFunction(ownedBy);
        try {
            final Map<String, String> data = vaultKVSecretEngine.readSecret(path);
            if (data == null || !data.containsKey("passphrase")) {
                return Optional.empty();
            }
            final String passphrase = data.get("passphrase");
            return Optional.of(new Passphrase(passphrase.toCharArray()));
        } catch (final VaultClientException vaultClientException) {
            if (Integer.valueOf(404).equals(vaultClientException.getStatus())) {
                return Optional.empty();
            } else {
                throw new UnableToRetrievePassphraseException(vaultClientException);
            }
        }
    }

    @Override
    public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException,
            UnableToStorePassphraseException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(passphrase);
        final String path = getOwnedByHasherStringBiFunction(ownedBy);
        final Map<String, String> secret = Map.of("passphrase", new String(passphrase.passphrase()));
        try {
            final Map<String, String> existing = vaultKVSecretEngine.readSecret(path);
            if (existing != null && existing.containsKey("passphrase")) {
                throw new PassphraseAlreadyExistsException(ownedBy);
            } else {
                vaultKVSecretEngine.writeSecret(path, secret);
                return new Passphrase(passphrase.passphrase().clone());
            }
        } catch (final VaultClientException vaultClientException) {
            if (Integer.valueOf(404).equals(vaultClientException.getStatus())) {
                // emitted by readSecret if the secret does not exist
                vaultKVSecretEngine.writeSecret(path, secret);
                return new Passphrase(passphrase.passphrase().clone());
            } else {
                throw new UnableToStorePassphraseException(vaultClientException);
            }
        }
    }

    private String getOwnedByHasherStringBiFunction(final OwnedBy ownedBy) {
        return ownedByToHashFunc.andThen(hashToPathFunc).apply(ownedBy, hasher);
    }

    private final BiFunction<OwnedBy, Hasher, Hash<OwnedBy>> ownedByToHashFunc =
            (ownedBy, hasher) -> hasher.hash(ownedBy);

    private final Function<Hash<OwnedBy>, String> hashToPathFunc = hash -> "secret/owner/" + hash.value();
}
