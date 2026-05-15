package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;

import java.util.Objects;

public class ConnectionIdentifierAssociation {

    private final ConnectionIdentifierRepository connectionIdentifierRepository;
    private final Hasher hasher;

    public ConnectionIdentifierAssociation(final ConnectionIdentifierRepository connectionIdentifierRepository, final Hasher hasher) {
        this.connectionIdentifierRepository = Objects.requireNonNull(connectionIdentifierRepository);
        this.hasher = Objects.requireNonNull(hasher);
    }

    public <A extends AggregateId> void associate(final ConnectionIdentifier connectionIdentifier, final A aggregateId) throws AlreadyAssociatedException {
        Objects.requireNonNull(connectionIdentifier);
        Objects.requireNonNull(aggregateId);
        final Hash<ConnectionIdentifier> connectionIdentifierHash = hasher.hash(connectionIdentifier);
        try {
            connectionIdentifierRepository.store(connectionIdentifierHash, aggregateId);
        } catch (final ConnectionIdentifierRepositoryException e) {
            throw new RuntimeException(e);
        } catch (final DuplicateConnectionIdentifierException e) {
            throw new AlreadyAssociatedException(connectionIdentifier);
        }
    }
}
