package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import org.apache.commons.lang3.Validate;

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
            final Optional<Passphrase> found = passphraseRepository.findBy(ownedBy);
            if (found.isEmpty()) {
                final Passphrase generated = passphraseGenerator.generate();
                Validate.validState(Status.VALID.equals(generated.status()));
                return passphraseRepository.store(ownedBy, generated);
            } else {
                final Passphrase passphrase = found.get();
                if (Status.BANNED.equals(passphrase.status())) {
                    throw new PassphraseBannedException();
                }
                return passphrase;
            }
        } catch (final UnableToRetrievePassphraseException
                       | PassphraseAlreadyExistsException
                       | UnableToStorePassphraseException
                       | PassphraseBannedException e) {
            throw new UnableToProvidePassphraseException(e);
        }
    }

    @Override
    public Passphrase ban(final OwnedBy ownedBy) throws UnableToBanPassphraseException {
        Objects.requireNonNull(ownedBy);
        try {
            final Passphrase passphrase = passphraseRepository.get(ownedBy);
            final Passphrase banned = passphrase.ban();
            passphraseRepository.update(ownedBy, banned);
            return banned;
        } catch (final UnableToRetrievePassphraseException | UnknownPassphraseException |
                       UnableToStorePassphraseException e) {
            throw new UnableToBanPassphraseException(e);
        }
    }
}
