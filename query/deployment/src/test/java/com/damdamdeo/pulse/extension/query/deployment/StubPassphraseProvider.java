package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.encryption.UnableToBanPassphraseException;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StubPassphraseProvider implements PassphraseProvider {

    @Override
    public Passphrase provide(final OwnedBy ownedBy) {
        return PassphraseSample.PASSPHRASE_1;
    }

    @Override
    public Passphrase ban(final OwnedBy ownedBy) throws UnableToBanPassphraseException {
        throw new IllegalStateException("Should not be called");
    }
}
