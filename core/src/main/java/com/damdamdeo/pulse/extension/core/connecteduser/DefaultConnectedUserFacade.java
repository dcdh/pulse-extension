package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.connectionidentifier.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;

import java.util.Objects;
import java.util.Optional;

public final class DefaultConnectedUserFacade implements ConnectedUserFacade {

    private final ConnectionIdentifierProvider connectionIdentifierProvider;
    private final ConnectionIdentifierRepository connectionIdentifierRepository;
    private final Hasher hasher;

    public DefaultConnectedUserFacade(final ConnectionIdentifierProvider connectionIdentifierProvider,
                                      final ConnectionIdentifierRepository connectionIdentifierRepository,
                                      final Hasher hasher) {
        this.connectionIdentifierProvider = Objects.requireNonNull(connectionIdentifierProvider);
        this.connectionIdentifierRepository = Objects.requireNonNull(connectionIdentifierRepository);
        this.hasher = Objects.requireNonNull(hasher);
    }

    @Override
    public Optional<AnyIdentifiable> isRegistered() throws RegistrationCheckerException {
        try {
            final ConnectionIdentifier connectionIdentifier = connectionIdentifierProvider.provide();
            return connectionIdentifierRepository.find(connectionIdentifier);
        } catch (final ConnectionIdentifierProviderException | ConnectionIdentifierRepositoryException exception) {
            throw new RegistrationCheckerException(exception);
        }
    }

    @Override
    public AnyIdentifiable getConnectedUser() throws ConnectedUserNotRegisteredException, ConnectedUserException {
        try {
            final ConnectionIdentifier connectionIdentifier = connectionIdentifierProvider.provide();
            return connectionIdentifierRepository.find(connectionIdentifier).orElseThrow(ConnectedUserNotRegisteredException::new);
        } catch (final ConnectionIdentifierProviderException | ConnectionIdentifierRepositoryException exception) {
            throw new ConnectedUserException(exception);
        }
    }

    @Override
    public Optional<AnyIdentifiable> findByExecutedBy(final ExecutedBy executedBy) throws ConnectedUserException {
        Objects.requireNonNull(executedBy);
        if (!executedBy.isEndUser()) {
            return Optional.empty();
        }
        try {
            return connectionIdentifierRepository.find(new ConnectionIdentifier(hasher.hash(
                    new ConnectedUser(executedBy.username())).value()));
        } catch (final ConnectionIdentifierRepositoryException exception) {
            throw new ConnectedUserException(exception);
        }
    }
}
