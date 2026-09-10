package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.traceability.From;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppenderException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class GuardQuery<I extends Input, P extends Projection> implements Query<I, P> {

    private final ExecutionContextProvider executionContextProvider;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;
    private final ExecutedByResolver executedByResolver;
    private final AggregateIdDecomposer aggregateIdDecomposer;
    private final Query<I, P> decorated;
    private final TraceAppender traceAppender;

    public GuardQuery(final ExecutionContextProvider executionContextProvider,
                      final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                      final ExecutedByResolver executedByResolver,
                      final Query<I, P> decorated,
                      final TraceAppender traceAppender) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
        this.executedByResolver = Objects.requireNonNull(executedByResolver);
        this.aggregateIdDecomposer = new AggregateIdDecomposer();
        this.decorated = Objects.requireNonNull(decorated);
        this.traceAppender = Objects.requireNonNull(traceAppender);
    }

    @Override
    public Result<P> execute(final I input) throws QueryException {
        Objects.requireNonNull(input);
        final List<Audience> audiences = decorated.audiences()
                .stream()
                .sorted(Comparator.comparing(Audience::priority))
                .toList();
        final ExecutionContext executionContext;
        if ((audiences.contains(Audience.ROLE_RESTRICTED) || audiences.contains(Audience.IN_EXECUTED_BY))
                && !audiences.contains(Audience.EVERYONE)) {
            executionContext = executionContextProvider.provide();
        } else {
            executionContext = null;
        }
        for (final Audience audience : audiences) {
            final Result<P> result = switch (audience) {
                case EVERYONE -> decorated.execute(input);
                case ROLE_RESTRICTED -> {
                    Objects.requireNonNull(executionContext);
                    final List<String> visibilityRoles = backendUserVisibilityRolesProvider.provide();
                    if (visibilityRoles.stream().anyMatch(executionContext::hasRole)) {
                        yield decorated.execute(input);
                    }
                    yield null;
                }
                case IN_EXECUTED_BY -> {
                    try {
                        Objects.requireNonNull(executionContext);
                        final Result<P> executed = decorated.execute(input);
                        final Set<AnyAggregateId> uncompounded = aggregateIdDecomposer.unCompound(executed.aggregateIds());
                        final Set<ExecutedBy> executedByEligibles = executedByResolver.resolve(uncompounded);
                        if (executedByEligibles.contains(executionContext.executedBy())) {
                            yield executed;
                        }
                        yield null;
                    } catch (final UnableToResolveException e) {
                        throw new QueryException(e, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
                    }
                }
            };
            if (result != null) {
                try {
                    traceAppender.append(result, From.from(input));
                } catch (final TraceAppenderException exception) {
                    throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
                }
                return result;
            }
        }
        throw new QueryException(new UnauthorizedException());
    }

    @Override
    public List<Audience> audiences() {
        return decorated.audiences();
    }
}
