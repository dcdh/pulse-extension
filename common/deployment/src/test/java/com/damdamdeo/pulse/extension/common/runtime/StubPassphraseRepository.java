package com.damdamdeo.pulse.extension.common.runtime;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class StubPassphraseRepository implements PassphraseRepository {

    @Override
    public Optional<Passphrase> retrieve(final OwnedBy ownedBy) {
        return Optional.of(PassphraseSample.PASSPHRASE_1);
    }

    @Override
    public List<RetrievedPassphrase> list(List<OwnedBy> multiples) throws UnableToRetrievePassphraseException {
        throw new IllegalStateException("Should not be called");
    }

    @Override
    public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
        throw new UnsupportedOperationException("Should not be called");
    }
}
