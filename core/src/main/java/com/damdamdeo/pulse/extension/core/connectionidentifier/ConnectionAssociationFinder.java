package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class ConnectionAssociationFinder {

    private final ConnectionIdentifierRepository connectionIdentifierRepository;

    public ConnectionAssociationFinder(final ConnectionIdentifierRepository connectionIdentifierRepository, final Hasher hasher) {
        this.connectionIdentifierRepository = Objects.requireNonNull(connectionIdentifierRepository);
    }

    public <A extends AggregateId> Optional<A> findByHash(final Hash<ConnectionIdentifier> connectionIdentifierHash, final Function<Identifiable, A> creational)
            throws UnableToFindByHashException {
        Objects.requireNonNull(connectionIdentifierHash);
        Objects.requireNonNull(creational);
        try {
            return connectionIdentifierRepository.findByHash(connectionIdentifierHash).map(creational);
        } catch (final ConnectionIdentifierRepositoryException e) {
            throw new UnableToFindByHashException(e, connectionIdentifierHash);
        }
    }
}
