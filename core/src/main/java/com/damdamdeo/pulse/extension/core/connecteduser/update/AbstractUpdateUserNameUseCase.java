package com.damdamdeo.pulse.extension.core.connecteduser.update;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.TechnicalException;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.connectionidentifier.*;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;

import java.util.Objects;

public abstract class AbstractUpdateUserNameUseCase<K extends AggregateId, C extends Command<K>, A extends AggregateRoot<K>> implements DomainUseCase<K, C, A> {

    private final CommandHandler<A, K> commandHandler;
    private final ConnectionIdentifierProvider connectionIdentifierProvider;
    private final ConnectionIdentifierRepository connectionIdentifierRepository;

    protected AbstractUpdateUserNameUseCase(final CommandHandler<A, K> commandHandler,
                                            final ConnectionIdentifierProvider connectionIdentifierProvider,
                                            final ConnectionIdentifierRepository connectionIdentifierRepository) {
        this.commandHandler = Objects.requireNonNull(commandHandler);
        this.connectionIdentifierProvider = Objects.requireNonNull(connectionIdentifierProvider);
        this.connectionIdentifierRepository = Objects.requireNonNull(connectionIdentifierRepository);
    }

    @Override
    public final A execute(final C updateUserNameCommand) throws BusinessException, TechnicalException {
        Objects.requireNonNull(updateUserNameCommand);
        try {
            final ConnectionIdentifier connectionIdentifier = connectionIdentifierProvider.provide();
            final A handled = commandHandler.handle(updateUserNameCommand, UnknownUserNameException::new);
            try {
                connectionIdentifierRepository.store(connectionIdentifier, handled.id());
            } catch (final DuplicateConnectionIdentifierException e) {
                // do nothing
            }
            onUserNameUpdated(handled, updateUserNameCommand);
            return handled;
        } catch (final ConnectionIdentifierProviderException | ConnectionIdentifierRepositoryException exception) {
            throw new TechnicalException(exception);
        }
    }

    protected abstract void onUserNameUpdated(A aggregateRoot, C updateUserNameCommand) throws BusinessException;
}
