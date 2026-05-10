package com.damdamdeo.pulse.extension.common.runtime.vault;

import com.damdamdeo.pulse.extension.common.runtime.hashing.AlgorithmQualifier;
import com.damdamdeo.pulse.extension.common.runtime.hashing.InternalHasher;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.hashing.Algorithm;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.client.VaultClientException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class VaultPassphraseRepository implements PassphraseRepository {

    private final VaultKVSecretEngine vaultKVSecretEngine;
    private final InternalHasher internalHasher;

    public VaultPassphraseRepository(final VaultKVSecretEngine vaultKVSecretEngine,
                                     @AlgorithmQualifier(Algorithm.SHA3_256) final InternalHasher internalHasher) {
        this.vaultKVSecretEngine = Objects.requireNonNull(vaultKVSecretEngine);
        this.internalHasher = Objects.requireNonNull(internalHasher);
    }

    @Override
    public Optional<Passphrase> retrieve(final OwnedBy ownedBy) {
        Objects.requireNonNull(ownedBy);
        final String path = computeSecretPathFromOwnedBy.apply(ownedBy);
        try {
            final Map<String, String> data = vaultKVSecretEngine.readSecret(path);
            if (data == null || !data.containsKey("passphrase")) {
                return Optional.empty();
            }
            final String passphrase = data.get("passphrase");
            return Optional.of(new Passphrase(passphrase.toCharArray()));
        } catch (final VaultClientException vaultClientException) {
            return Optional.empty();
        }
    }

    @Override
    public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(passphrase);
        final String path = computeSecretPathFromOwnedBy.apply(ownedBy);
        final Map<String, String> secret = Map.of("passphrase", new String(passphrase.passphrase()));
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
        return new Passphrase(passphrase.passphrase().clone());
    }

    private Function<OwnedBy, String> computeSecretPathFromOwnedBy = new Function<OwnedBy, Hash>() {
        @Override
        public Hash apply(final OwnedBy ownedBy) {
            return internalHasher.hash(ownedBy.id());
        }
    }.andThen(hash -> "secret/owner/" + hash.hashed());
}
