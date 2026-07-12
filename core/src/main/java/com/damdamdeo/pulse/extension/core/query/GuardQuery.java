package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class GuardQuery<I, P extends Projection> implements Query<I, P> {

    private final ExecutionContextProvider executionContextProvider;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;
    private final ExecutedByResolver executedByResolver;
    private final AggregateIdDecomposer aggregateIdDecomposer;
    private final Query<I, P> decorated;

    public GuardQuery(final ExecutionContextProvider executionContextProvider,
                      final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                      final ExecutedByResolver executedByResolver,
                      final Query<I, P> decorated) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
        this.executedByResolver = Objects.requireNonNull(executedByResolver);
        this.aggregateIdDecomposer = new AggregateIdDecomposer();
        this.decorated = Objects.requireNonNull(decorated);
    }

    @Override
    public Result<P> execute(final I input) throws QueryException {
        Objects.requireNonNull(input);
        final List<Audience> audiences = decorated.audiences();
        for (final Audience audience : audiences) {
            final Result<P> result = switch (audience) {
                case EVERYONE -> decorated.execute(input);
                case ROLE_RESTRICTED -> {
                    final ExecutionContext provide = executionContextProvider.provide();
                    final List<String> visibilityRoles = backendUserVisibilityRolesProvider.provide();
                    if (visibilityRoles.stream().anyMatch(provide::hasRole)) {
                        yield decorated.execute(input);
                    }
                    yield null;
                }
                case IN_EXECUTED_BY -> {
                    final Result<P> executed = decorated.execute(input);
                    final Set<AggregateId> uncompounded = aggregateIdDecomposer.unCompound(executed.aggregateIds());
                    final Set<ExecutedBy> executedByEligibles = executedByResolver.resolve(uncompounded);
                    final ExecutionContext executionContext = executionContextProvider.provide();
                    if (executedByEligibles.contains(executionContext.executedBy())) {
                        yield executed;
                    }
                    yield null;
                }
            };
            if (result != null) {
                return result;
            }
        }
        throw new QueryException(new DisallowException());
    }

    @Override
    public List<Audience> audiences() {
        return decorated.audiences();
    }
}
