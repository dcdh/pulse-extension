package com.damdamdeo.pulse.extension.core.connecteduser.registration;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreationalCommand;
import com.damdamdeo.pulse.extension.core.connectionidentifier.*;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;

import java.util.Objects;

public abstract class AbstractRegistrationDomainUseCase<K extends AggregateId, C extends CreationalCommand<K>, A extends AggregateRoot<K>> implements DomainUseCase<K, C, A> {

    private final CommandHandler<A, K> commandHandler;
    private final ConnectionIdentifierProvider connectionIdentifierProvider;
    private final ConnectionIdentifierRepository connectionIdentifierRepository;

    protected AbstractRegistrationDomainUseCase(final CommandHandler<A, K> commandHandler,
                                                final ConnectionIdentifierProvider connectionIdentifierProvider,
                                                final ConnectionIdentifierRepository connectionIdentifierRepository) {
        this.commandHandler = Objects.requireNonNull(commandHandler);
        this.connectionIdentifierProvider = Objects.requireNonNull(connectionIdentifierProvider);
        this.connectionIdentifierRepository = Objects.requireNonNull(connectionIdentifierRepository);
    }

    @Override
    public final A execute(final C registrationCommand) throws BusinessException, TechnicalException {
        Objects.requireNonNull(registrationCommand);
        try {
            final ConnectionIdentifier connectionIdentifier = connectionIdentifierProvider.provide();
            final A handled = commandHandler.handle(this::from, registrationCommand, k -> new ConnectedUserAlreadyRegisteredException());
            connectionIdentifierRepository.store(connectionIdentifier, handled.id());
            onUserNameRegistered(handled, registrationCommand);
            return handled;
        } catch (final ConnectionIdentifierProviderException | ConnectionIdentifierRepositoryException exception) {
            throw new TechnicalException(exception);
        } catch (final DuplicateConnectionIdentifierException e) {
            throw new BusinessException(e);
        }
    }

    protected abstract K from(SequenceNumber sequenceNumber);

    protected abstract void onUserNameRegistered(A aggregateRoot, C registrationCommand) throws BusinessException;
}
