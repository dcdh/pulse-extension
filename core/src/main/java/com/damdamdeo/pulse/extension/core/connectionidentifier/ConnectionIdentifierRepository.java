package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Optional;

public interface ConnectionIdentifierRepository {

    ConnectionIdentifier store(ConnectionIdentifier connectionIdentifier, Identifiable identifiable)
            throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException;

    Optional<Identifiable> find(final ConnectionIdentifier connectionIdentifier)
            throws ConnectionIdentifierRepositoryException;
}
