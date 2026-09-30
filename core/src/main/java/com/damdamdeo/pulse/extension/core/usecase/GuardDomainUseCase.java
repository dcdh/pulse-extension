package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.Prioritable;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.command.AggregateIdTraceable;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.command.CreationalCommand;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.permission.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.permission.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.AggregateIdDecomposer;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.core.usecase.permission.Permission;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public abstract class GuardDomainUseCase<K extends AggregateId, C extends Command<K>, A extends AggregateRoot<K>> implements DomainUseCase<K, C, A> {

    private final ExecutionContextProvider executionContextProvider;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;
    private final ExecutedByResolver executedByResolver;
    private final AggregateIdDecomposer aggregateIdDecomposer;
    private final DomainUseCase<K, C, A> decorated;
    private final DistributedLockManager distributedLockManager;
    private final TraceAppender traceAppender;

    public GuardDomainUseCase(final ExecutionContextProvider executionContextProvider,
                              final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                              final ExecutedByResolver executedByResolver,
                              final AggregateIdDecomposer aggregateIdDecomposer,
                              final DomainUseCase<K, C, A> decorated,
                              final DistributedLockManager distributedLockManager,
                              final TraceAppender traceAppender) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
        this.executedByResolver = Objects.requireNonNull(executedByResolver);
        this.aggregateIdDecomposer = Objects.requireNonNull(aggregateIdDecomposer);
        this.decorated = Objects.requireNonNull(decorated);
        this.distributedLockManager = Objects.requireNonNull(distributedLockManager);
        this.traceAppender = Objects.requireNonNull(traceAppender);
    }

    @Override
    public final A execute(final C command) throws UseCaseException {
        Objects.requireNonNull(command);
        final List<Permission<K, C>> permissions = decorated.permissions()
                .stream()
                .sorted(Comparator.comparing(Prioritable::priority))
                .toList();
        final PermissionExecutionContext context = new PermissionExecutionContext(executionContextProvider,
                backendUserVisibilityRolesProvider, executedByResolver, aggregateIdDecomposer);
        // TODO avoid instanceof
        try {
            if (command instanceof CreationalCommand<?>) {
                for (final Permission<K, C> permission : permissions) {
                    if (permission.allow(command, context)) {
                        final A executed = decorated.execute(command);
                        traceAppender.append(new AggregateIdTraceable<>(executed.id()), Source.COMMAND, ExecutionStatus.SUCCESS,
                                From.from(command));
                        return executed;
                    }
                }
                throw new UnauthorizedException();
            } else {
                try {
                    for (final Permission<K, C> permission : permissions) {
                        if (permission.allow(command.id(), command, context)) {
                            try {
                                final A executed = distributedLockManager.executeWithLock(command, new UseCaseExecutor<K, C, A>() {
                                    @Override
                                    public A execute(final C command) throws UseCaseException {
                                        Objects.requireNonNull(command);
                                        return decorated.execute(command);
                                    }
                                });
                                traceAppender.append(new AggregateIdTraceable<>(executed.id()), Source.COMMAND, ExecutionStatus.SUCCESS,
                                        From.from(command));
                                return executed;
                            } catch (final LockingException exception) {
                                throw new UseCaseException(exception, UseCaseExceptionCode.INFRASTRUCTURE_FAILURE);
                            }
                        }
                    }
                    traceAppender.append(new AggregateIdTraceable<>(command.id()), Source.COMMAND, ExecutionStatus.FAILED_UNAUTHORIZED,
                            From.from(command));
                    throw new UnauthorizedException();
                } catch (final UseCaseException exception) {
                    if (UseCaseExceptionCode.BUSINESS_FAILURE.equals(exception.useCaseExceptionCode())) {
                        traceAppender.append(new AggregateIdTraceable<>(command.id()), Source.COMMAND, ExecutionStatus.FAILED_BUSINESS,
                                From.from(command));
                    }
                    throw exception;
                }
            }
        } catch (final UnauthorizedException exception) {
            throw new UseCaseException(new UnauthorizedException());
        } catch (final TraceAppenderException exception) {
            throw new UseCaseException(exception, UseCaseExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override
    public List<Permission<K, C>> permissions() {
        return decorated.permissions();
    }
}
