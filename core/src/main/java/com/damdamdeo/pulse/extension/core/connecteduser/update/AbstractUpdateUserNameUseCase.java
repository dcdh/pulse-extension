package com.damdamdeo.pulse.extension.core.connecteduser.update;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.command.CommandException;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.Handled;
import com.damdamdeo.pulse.extension.core.connectionidentifier.*;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseExceptionCode;

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
    public final Handled<A, K> execute(final C updateUserNameCommand) throws UseCaseException {
        Objects.requireNonNull(updateUserNameCommand);
        try {
            final ConnectionIdentifier connectionIdentifier = connectionIdentifierProvider.provide();
            final Handled<A, K> handled = commandHandler.handle(updateUserNameCommand, UnknownUserNameException::new);
            try {
                connectionIdentifierRepository.store(connectionIdentifier, handled.id());
            } catch (final DuplicateConnectionIdentifierException e) {
                // do nothing
            }
            onUserNameUpdated(handled, updateUserNameCommand);
            return handled;
        } catch (final ConnectionIdentifierProviderException | ConnectionIdentifierRepositoryException exception) {
            throw new UseCaseException(exception, UseCaseExceptionCode.INFRASTRUCTURE_FAILURE);
        } catch (final CommandException commandException) {
            final UseCaseExceptionCode useCaseExceptionCode = switch (commandException.commandExceptionCode()) {
                case BUSINESS_FAILURE -> UseCaseExceptionCode.BUSINESS_FAILURE;
                case INFRASTRUCTURE_FAILURE -> UseCaseExceptionCode.INFRASTRUCTURE_FAILURE;
            };
            throw new UseCaseException(commandException, useCaseExceptionCode);
        }
    }

    protected abstract void onUserNameUpdated(Handled<A, K> handled, C updateUserNameCommand) throws UseCaseException;
}
