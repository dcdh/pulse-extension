package com.damdamdeo.pulse.extension.common.runtime;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.List;
import java.util.Optional;

// remove in favor to com.damdamdeo.pulse.extension.encryption.storage.deployment.StubPassphraseRepository;
@Deprecated(forRemoval = true)
@ApplicationScoped
@Priority(1)
@Alternative
public class StubPassphraseRepository implements PassphraseRepository {

    @Override
    public Optional<Passphrase> findBy(final OwnedBy ownedBy) {
        return Optional.of(PassphraseSample.PASSPHRASE_1);
    }

    @Override
    public Passphrase get(OwnedBy ownedBy) throws UnableToRetrievePassphraseException, UnknownPassphraseException {
        throw new IllegalStateException("Should not be called");
    }

    @Override
    public List<RetrievedPassphrase> list(List<OwnedBy> multiples) throws UnableToRetrievePassphraseException {
        throw new IllegalStateException("Should not be called");
    }

    @Override
    public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
        throw new IllegalStateException("Should not be called");
    }

    @Override
    public Passphrase update(OwnedBy ownedBy, Passphrase passphrase) throws UnableToStorePassphraseException, UnknownPassphraseException {
        throw new IllegalStateException("Should not be called");
    }
}
