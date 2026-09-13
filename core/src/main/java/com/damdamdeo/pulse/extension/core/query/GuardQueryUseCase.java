package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.audience.Audience;
import com.damdamdeo.pulse.extension.core.query.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.traceability.From;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppenderException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class GuardQueryUseCase<I extends Input, P extends Projection> implements QueryUseCase<I, P> {

    private final ExecutionContextProvider executionContextProvider;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;
    private final ExecutedByResolver executedByResolver;
    private final AggregateIdDecomposer aggregateIdDecomposer;
    private final QueryUseCase<I, P> decorated;
    private final TraceAppender traceAppender;

    public GuardQueryUseCase(final ExecutionContextProvider executionContextProvider,
                             final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                             final ExecutedByResolver executedByResolver,
                             final QueryUseCase<I, P> decorated,
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
        final AudienceExecutionContext context = new AudienceExecutionContext(executionContextProvider,
                backendUserVisibilityRolesProvider, executedByResolver, aggregateIdDecomposer);
        for (final Audience audience : audiences) {
            final Optional<Result<P>> result = audience.execute(input, decorated, context);
            if (result.isPresent()) {
                try {
                    traceAppender.append(result.get(), From.from(input));
                } catch (final TraceAppenderException exception) {
                    throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
                }
                return result.get();
            }
        }
        throw new QueryException(new UnauthorizedException());
    }

    @Override
    public List<Audience> audiences() {
        return decorated.audiences();
    }
}
