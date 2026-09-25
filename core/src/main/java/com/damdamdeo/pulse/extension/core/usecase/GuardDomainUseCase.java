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
import com.damdamdeo.pulse.extension.core.traceability.From;
import com.damdamdeo.pulse.extension.core.traceability.Source;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppenderException;
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
    private final TraceAppender traceAppender;

    public GuardDomainUseCase(final ExecutionContextProvider executionContextProvider,
                              final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                              final ExecutedByResolver executedByResolver,
                              final AggregateIdDecomposer aggregateIdDecomposer,
                              final DomainUseCase<K, C, A> decorated,
                              final TraceAppender traceAppender) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
        this.executedByResolver = Objects.requireNonNull(executedByResolver);
        this.aggregateIdDecomposer = Objects.requireNonNull(aggregateIdDecomposer);
        this.decorated = Objects.requireNonNull(decorated);
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
        boolean allowed = false;
        if (command instanceof CreationalCommand<?>) {
            for (final Permission<K, C> permission : permissions) {
                if (permission.allow(command, context)) {
                    allowed = true;
                    break;
                }
            }
        } else {
            for (final Permission<K, C> permission : permissions) {
                if (permission.allow(command.id(), command, context)) {
                    allowed = true;
                    break;
                }
            }
        }
        try {
            if (allowed) {
                final A executed = decorated.execute(command);
                traceAppender.append(new AggregateIdTraceable(executed.id()), Source.COMMAND, From.from(command));
                return executed;
            } else {
                // TODO store unauthorized traceAppender
                throw new UseCaseException(new UnauthorizedException());
            }
        } catch (final TraceAppenderException exception) {
            throw new UseCaseException(exception, UseCaseExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override
    public List<Permission<K, C>> permissions() {
        return decorated.permissions();
    }
}
