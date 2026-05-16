package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateIdGenerator;
import com.damdamdeo.pulse.extension.core.SequenceGenerationException;
import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.damdamdeo.pulse.extension.core.connectionidentifier.AlreadyAssociatedException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionAssociationFinder;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierAssociation;
import com.damdamdeo.pulse.extension.core.connectionidentifier.UnableToFindByHashException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class ConnectedUserAggregateIdProvider {

    private final ConnectedUserProvider connectedUserProvider;
    private final AggregateIdGenerator aggregateIdGenerator;
    private final ConnectionAssociationFinder connectionAssociationFinder;
    private final ConnectionIdentifierAssociation connectionIdentifierAssociation;
    private final Hasher hasher;

    public ConnectedUserAggregateIdProvider(final ConnectedUserProvider connectedUserProvider,
                                            final AggregateIdGenerator aggregateIdGenerator,
                                            final ConnectionAssociationFinder connectionAssociationFinder,
                                            final ConnectionIdentifierAssociation connectionIdentifierAssociation,
                                            final Hasher hasher) {
        this.connectedUserProvider = Objects.requireNonNull(connectedUserProvider);
        this.aggregateIdGenerator = Objects.requireNonNull(aggregateIdGenerator);
        this.connectionAssociationFinder = Objects.requireNonNull(connectionAssociationFinder);
        this.connectionIdentifierAssociation = Objects.requireNonNull(connectionIdentifierAssociation);
        this.hasher = Objects.requireNonNull(hasher);
    }

    public <A extends AggregateId> A provide(final Class<A> clazz,
                                             final Function<Identifiable, A> identifiableToAggregateIdFunction,
                                             final Function<SequenceNumber, A> sequenceNumberAggregateIdFunction) throws ConnectedUserAggregateIdProviderException {
        try {
            final ConnectedUser connectedUser = connectedUserProvider.provide();
            final Optional<A> byHash = connectionAssociationFinder.findByHash(hasher.hash(connectedUser), identifiableToAggregateIdFunction);
            if (byHash.isPresent()) {
                return byHash.get();
            } else {
                final A generated = aggregateIdGenerator.generate(clazz, sequenceNumberAggregateIdFunction);
                connectionIdentifierAssociation.associate(connectedUser, generated);
                return generated;
            }
        } catch (final UnableToFindByHashException | SequenceGenerationException | AlreadyAssociatedException e) {
            throw new ConnectedUserAggregateIdProviderException(e);
        }
    }
}
