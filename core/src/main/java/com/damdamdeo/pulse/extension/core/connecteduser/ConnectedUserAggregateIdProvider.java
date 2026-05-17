package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateIdGenerator;
import com.damdamdeo.pulse.extension.core.SequenceGenerationException;
import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.damdamdeo.pulse.extension.core.connectionidentifier.AlreadyAssociatedException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionAssociationFinder;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierAssociation;
import com.damdamdeo.pulse.extension.core.connectionidentifier.UnableToFindException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;
import java.util.function.Function;

public class ConnectedUserAggregateIdProvider {

    private final AggregateIdGenerator aggregateIdGenerator;
    private final ConnectionAssociationFinder connectionAssociationFinder;
    private final ConnectionIdentifierAssociation connectionIdentifierAssociation;

    public ConnectedUserAggregateIdProvider(final AggregateIdGenerator aggregateIdGenerator,
                                            final ConnectionAssociationFinder connectionAssociationFinder,
                                            final ConnectionIdentifierAssociation connectionIdentifierAssociation) {
        this.aggregateIdGenerator = Objects.requireNonNull(aggregateIdGenerator);
        this.connectionAssociationFinder = Objects.requireNonNull(connectionAssociationFinder);
        this.connectionIdentifierAssociation = Objects.requireNonNull(connectionIdentifierAssociation);
    }

    public <A extends AggregateId> A provide(final Class<A> clazz,
                                             final Function<Identifiable, A> identifiableToAggregateIdFunction,
                                             final Function<SequenceNumber, A> sequenceNumberAggregateIdFunction) throws ConnectedUserAggregateIdProviderException {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(identifiableToAggregateIdFunction);
        Objects.requireNonNull(sequenceNumberAggregateIdFunction);
        try {
            final Provided<A> provided = connectionAssociationFinder.findByConnectedUser(identifiableToAggregateIdFunction);
            if (!provided.isUnknown()) {
                return provided.identifiable();
            } else {
                final A generated = aggregateIdGenerator.generate(clazz, sequenceNumberAggregateIdFunction);
                connectionIdentifierAssociation.associate(provided.connectedUser(), generated);
                return generated;
            }
        } catch (final UnableToFindException | SequenceGenerationException | AlreadyAssociatedException e) {
            throw new ConnectedUserAggregateIdProviderException(e);
        }
    }
}
