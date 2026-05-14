package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hash;

import java.util.Optional;

public interface ConnectionIdentifierRepository {

    void store(Hash<ConnectionIdentifier> connectionIdentifierHash, Identifiable identifiable)
            throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException;

    Optional<Identifiable> findByHash(final Hash<ConnectionIdentifier> connectionIdentifierHash)
            throws ConnectionIdentifierRepositoryException;
}
