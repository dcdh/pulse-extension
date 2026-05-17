package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.connecteduser.*;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;

import java.util.Objects;
import java.util.function.Function;

public class ConnectionAssociationFinder {

    private final ConnectedUserProvider connectedUserProvider;
    private final ConnectionIdentifierRepository connectionIdentifierRepository;
    private final Hasher hasher;

    public ConnectionAssociationFinder(final ConnectedUserProvider connectedUserProvider,
                                       final ConnectionIdentifierRepository connectionIdentifierRepository,
                                       final Hasher hasher) {
        this.connectedUserProvider = Objects.requireNonNull(connectedUserProvider);
        this.connectionIdentifierRepository = Objects.requireNonNull(connectionIdentifierRepository);
        this.hasher = Objects.requireNonNull(hasher);
    }

    public <A extends Identifiable> Provided<A> findByConnectedUser(final Function<Identifiable, A> creational)
            throws UnableToFindException {
        Objects.requireNonNull(creational);
        try {
            final ConnectedUser connectedUser = connectedUserProvider.provide();
            return connectionIdentifierRepository.findByHash(hasher.hash(connectedUser)).map(creational)
                    .map(aggregateId -> Provided.ofKnown(aggregateId, connectedUser))
                    .orElseGet(() -> Provided.ofUnknown(connectedUser));
        } catch (final ConnectionIdentifierRepositoryException | ConnectedIsAnonymousException |
                       UsernameNotAMailException | ConnectedUserNotAvailableException exception) {
            throw new UnableToFindException(exception);
        }
    }
}
