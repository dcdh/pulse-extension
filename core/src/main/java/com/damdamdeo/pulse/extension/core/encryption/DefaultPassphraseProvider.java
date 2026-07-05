package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;
import java.util.Optional;

public class DefaultPassphraseProvider implements PassphraseProvider {

    private final PassphraseRepository passphraseRepository;
    private final PassphraseGenerator passphraseGenerator;

    public DefaultPassphraseProvider(final PassphraseRepository passphraseRepository,
                                     final PassphraseGenerator passphraseGenerator) {
        this.passphraseRepository = Objects.requireNonNull(passphraseRepository);
        this.passphraseGenerator = Objects.requireNonNull(passphraseGenerator);
    }

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
