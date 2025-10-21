package com.damdamdeo.pulse.extension.runtime.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Optional;

public interface PassphraseRepository {

    Optional<char[]> retrieve(OwnedBy ownedBy);

    char[] store(OwnedBy ownedBy, char[] key) throws PassphraseAlreadyExistsException;
}
