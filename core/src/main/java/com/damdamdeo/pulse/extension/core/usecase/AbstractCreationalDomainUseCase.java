package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.DuplicateAggregateException;
import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.damdamdeo.pulse.extension.core.command.CommandException;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreationalCommand;

import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractCreationalDomainUseCase<K extends AggregateId, C extends CreationalCommand<K>, A extends AggregateRoot<K>>
        implements DomainUseCase<K, C, A> {

    private final CommandHandler<A, K> commandHandler;

    protected AbstractCreationalDomainUseCase(final CommandHandler<A, K> commandHandler) {
        this.commandHandler = Objects.requireNonNull(commandHandler);
    }

    @Override
    public final A execute(final C command) throws UseCaseException {
        Objects.requireNonNull(command);
        try {
            final C processedCommand = onBefore(command);
            final A handled = commandHandler.handle(creational(processedCommand), processedCommand, duplicateAggregateException());
            return onAfter(command, handled);
        } catch (final UseCaseExecutionException useCaseExecutionException) {
            throw new UseCaseException(useCaseExecutionException, useCaseExecutionException.useCaseExceptionCode());
        } catch (final CommandException commandException) {
            final UseCaseExceptionCode useCaseExceptionCode = switch (commandException.commandExceptionCode()) {
                case BUSINESS_FAILURE -> UseCaseExceptionCode.BUSINESS_FAILURE;
                case INFRASTRUCTURE_FAILURE -> UseCaseExceptionCode.INFRASTRUCTURE_FAILURE;
            };
            throw new UseCaseException(commandException, useCaseExceptionCode);
        }
    }

    /**
     * Implements this method to provide the creational function.
     *
     * @param command to add context to aggregate id
     * @return
     */
    protected abstract Function<SequenceNumber, K> creational(C command);

    /**
     * Implements this method to provide the duplicate aggregate exception function.
     *
     * @return
     */
    protected abstract Function<K, DuplicateAggregateException> duplicateAggregateException();

    /**
     * Override this method to perform any pre-processing before the command is handled.
     *
     * @param command
     * @return
     * @throws UseCaseExecutionException
     */
    protected C onBefore(final C command) throws UseCaseExecutionException {
        Objects.requireNonNull(command);
        return command;
    }

    /**
     * Override this method to perform any post-processing after the command is handled.
     *
     * @param command
     * @param aggregate
     * @return
     * @throws UseCaseExecutionException
     */
    protected A onAfter(final C command, final A aggregate) throws UseCaseExecutionException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(aggregate);
        return aggregate;
    }
}
