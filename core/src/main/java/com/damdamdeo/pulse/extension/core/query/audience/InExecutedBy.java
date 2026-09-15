package com.damdamdeo.pulse.extension.core.query.audience;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.*;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class InExecutedBy implements Audience {

    public static final InExecutedBy INSTANCE = new InExecutedBy();

    private InExecutedBy() {
    }

    @Override
    public <I extends Input, P extends Projection> Optional<Result<P>> execute(final I input, final QueryUseCase<I, P> decorated,
                                                                               final AudienceExecutionContext audienceExecutionContext)
            throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(decorated);
        Objects.requireNonNull(audienceExecutionContext);
        try {
            final ExecutionContext executionContext = audienceExecutionContext.executionContextProvider().provide();
            final Result<P> executed = decorated.execute(input);
            final Set<AggregateId> uncompounded = audienceExecutionContext.aggregateIdDecomposer().unCompound(executed.aggregateIds());
            final Set<ExecutedBy> executedByEligibles = audienceExecutionContext.executedByResolver().resolve(uncompounded);
            if (executedByEligibles.contains(executionContext.executedBy())) {
                return Optional.of(executed);
            }
            return Optional.empty();
        } catch (final UnableToResolveException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override
    public int priority() {
        return 2;
    }
}
