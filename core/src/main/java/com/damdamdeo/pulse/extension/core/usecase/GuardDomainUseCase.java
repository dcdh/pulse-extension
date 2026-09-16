package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.Prioritable;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.command.CreationalCommand;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.AggregateIdDecomposer;
import com.damdamdeo.pulse.extension.core.query.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.query.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.usecase.audience.Audience;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public abstract non-sealed class GuardDomainUseCase<K extends AggregateId, C extends Command<K>, A extends AggregateRoot<K>> implements DomainUseCase<K, C, A> {

    private final ExecutionContextProvider executionContextProvider;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;
    private final ExecutedByResolver executedByResolver;
    private final AggregateIdDecomposer aggregateIdDecomposer;
    private final DomainUseCase<K, C, A> decorated;

    public GuardDomainUseCase(final ExecutionContextProvider executionContextProvider,
                              final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                              final ExecutedByResolver executedByResolver,
                              final AggregateIdDecomposer aggregateIdDecomposer,
                              final DomainUseCase<K, C, A> decorated) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
        this.executedByResolver = Objects.requireNonNull(executedByResolver);
        this.aggregateIdDecomposer = Objects.requireNonNull(aggregateIdDecomposer);
        this.decorated = Objects.requireNonNull(decorated);
    }

    @Override
    public final A execute(final C command) throws UseCaseException {
        Objects.requireNonNull(command);
        final List<Audience> audiences = decorated.audiences()
                .stream()
                .sorted(Comparator.comparing(Prioritable::priority))
                .toList();
        final AudienceExecutionContext context = new AudienceExecutionContext(executionContextProvider,
                backendUserVisibilityRolesProvider, executedByResolver, aggregateIdDecomposer);
        // TODO avoid instanceof
        if (command instanceof CreationalCommand<?>) {
            for (final Audience audience : audiences) {
                if (audience.allow(context)) {
                    return decorated.execute(command);
                }
            }
        } else {
            for (final Audience audience : audiences) {
                if (audience.allow(command.id(), context)) {
                    return decorated.execute(command);
                }
            }
        }
        throw new UseCaseException(new UnauthorizedException());
    }

    @Override
    public List<Audience> audiences() {
        return decorated.audiences();
    }
}
