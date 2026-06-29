package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.TechnicalException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.*;
import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;
import java.util.Optional;

public final class DefaultConnectedUserFacade implements ConnectedUserFacade {

    private final ConnectionIdentifierProvider connectionIdentifierProvider;
    private final ConnectionIdentifierRepository connectionIdentifierRepository;

    public DefaultConnectedUserFacade(final ConnectionIdentifierProvider connectionIdentifierProvider,
                                      final ConnectionIdentifierRepository connectionIdentifierRepository) {
        this.connectionIdentifierProvider = Objects.requireNonNull(connectionIdentifierProvider);
        this.connectionIdentifierRepository = Objects.requireNonNull(connectionIdentifierRepository);
    }

    @Override
    public Optional<Identifiable> isRegistered() throws TechnicalException {
        try {
            final ConnectionIdentifier connectionIdentifier = connectionIdentifierProvider.provide();
            return connectionIdentifierRepository.find(connectionIdentifier);
        } catch (final ConnectionIdentifierProviderException | ConnectionIdentifierRepositoryException exception) {
            throw new TechnicalException(exception);
        }
    }
}
