package com.damdamdeo.pulse.extension.encryption.storage.runtime;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.client.VaultClientException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class VaultPassphraseRepository implements PassphraseRepository {

    private static final String PASSPHRASE = "passphrase";

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
            if (data == null || !data.containsKey(PASSPHRASE)) {
                return Optional.empty();
            }
            final String passphrase = data.get(PASSPHRASE);
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
    public List<RetrievedPassphrase> retrieve(final List<OwnedBy> multiples) throws UnableToRetrievePassphraseException {
        Objects.requireNonNull(multiples);
        final List<RetrievedPassphrase> retrievedPassphrases = new ArrayList<>(multiples.size());
        for (final OwnedBy ownedBy : multiples) {
            final Optional<Passphrase> retrieved = retrieve(ownedBy);
            if (retrieved.isPresent()) {
                retrievedPassphrases.add(new RetrievedPassphrase(ownedBy, retrieved.get()));
            } else {
                retrievedPassphrases.add(new RetrievedPassphrase(ownedBy, null));
            }
        }
        return retrievedPassphrases;
    }

    @Override
    public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException,
            UnableToStorePassphraseException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(passphrase);
        final String path = getOwnedByHasherStringBiFunction(ownedBy);
        final Map<String, String> secret = Map.of(PASSPHRASE, new String(passphrase.passphrase()));
        try {
            final Map<String, String> existing = vaultKVSecretEngine.readSecret(path);
            if (existing != null && existing.containsKey(PASSPHRASE)) {
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

    // TODO prendre en compte le context application via le nom
    private final Function<Hash<OwnedBy>, String> hashToPathFunc = hash -> "owner/" + hash.value();
}
