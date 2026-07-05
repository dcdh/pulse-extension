package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.List;
import java.util.Optional;

public interface PassphraseRepository {

    Optional<Passphrase> findBy(OwnedBy ownedBy) throws UnableToRetrievePassphraseException;

    Passphrase get(OwnedBy ownedBy) throws UnableToRetrievePassphraseException, UnknownPassphraseException;

    List<RetrievedPassphrase> list(List<OwnedBy> multiples) throws UnableToRetrievePassphraseException;

    Passphrase store(OwnedBy ownedBy, Passphrase passphrase) throws PassphraseAlreadyExistsException, UnableToStorePassphraseException;

    Passphrase update(OwnedBy ownedBy, Passphrase passphrase) throws UnableToStorePassphraseException, UnknownPassphraseException;
}
