package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.MissingAggregateException;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.command.CommandException;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;

import java.util.Objects;
import java.util.function.Supplier;

public abstract non-sealed class AbstractDomainUseCase<K extends AggregateId, C extends Command<K>, A extends AggregateRoot<K>>
        implements DomainUseCase<K, C, A> {

    private final CommandHandler<A, K> commandHandler;

    protected AbstractDomainUseCase(final CommandHandler<A, K> commandHandler) {
        this.commandHandler = Objects.requireNonNull(commandHandler);
    }

    @Override
    public final A execute(final C command) throws UseCaseException {
        Objects.requireNonNull(command);
        try {
            onBefore(command);
            final A handled = commandHandler.handle(command, missingAggregateException());
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

    protected abstract Supplier<MissingAggregateException> missingAggregateException();

    /**
     * Override this method to perform any pre-processing before the command is handled.
     *
     * @param command
     * @throws UseCaseExecutionException
     */
    protected void onBefore(final C command) throws UseCaseExecutionException {
        Objects.requireNonNull(command);
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
