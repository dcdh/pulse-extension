package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Optional;

public interface PassphraseRepository {

    Optional<Passphrase> retrieve(OwnedBy ownedBy);

    Passphrase store(OwnedBy ownedBy, Passphrase passphrase) throws PassphraseAlreadyExistsException;
}
