package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class DefaultPassphraseProvider implements PassphraseProvider {

    private final PassphraseRepository passphraseRepository;
    private final PassphraseGenerator passphraseGenerator;

    public DefaultPassphraseProvider(final PassphraseRepository passphraseRepository,
                                     final PassphraseGenerator passphraseGenerator) {
        this.passphraseRepository = Objects.requireNonNull(passphraseRepository);
        this.passphraseGenerator = Objects.requireNonNull(passphraseGenerator);
    }

    // https://github.com/quarkusio/quarkus/issues/19676
    @CacheResult(cacheName = "passphrase")
    @Override
    public Passphrase provide(final OwnedBy ownedBy) throws UnableToProvidePassphraseException {
        Objects.requireNonNull(ownedBy);
        try {
            final Optional<Passphrase> retrieved = passphraseRepository.retrieve(ownedBy);
            if (retrieved.isPresent()) {
                return retrieved.get();
            } else {
                final Passphrase generated = passphraseGenerator.generate();
                return passphraseRepository.store(ownedBy, generated);
            }
        } catch (final UnableToRetrievePassphraseException | PassphraseAlreadyExistsException |
                       UnableToStorePassphraseException exception) {
            throw new UnableToProvidePassphraseException(exception);
        }
    }
}
