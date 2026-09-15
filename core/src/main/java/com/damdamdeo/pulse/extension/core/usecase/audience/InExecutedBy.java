package com.damdamdeo.pulse.extension.core.usecase.audience;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.UnableToResolveException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseExceptionCode;

import java.util.Objects;
import java.util.Set;

public final class InExecutedBy implements Audience {

    public static final InExecutedBy INSTANCE = new InExecutedBy();

    private InExecutedBy() {
    }

    @Override
    public boolean allow(final AggregateId aggregateId, final AudienceExecutionContext audienceExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(audienceExecutionContext);
        try {
            final ExecutionContext executionContext = audienceExecutionContext.executionContextProvider().provide();
            final Set<AggregateId> uncompounded = audienceExecutionContext.aggregateIdDecomposer().unCompound(Set.of(aggregateId));
            final Set<ExecutedBy> executedByEligibles = audienceExecutionContext.executedByResolver().resolve(uncompounded);
            return executedByEligibles.contains(executionContext.executedBy());
        } catch (final UnableToResolveException exception) {
            throw new UseCaseException(exception, UseCaseExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override
    public boolean allow(final AudienceExecutionContext audienceExecutionContext) throws UseCaseException {
        Objects.requireNonNull(audienceExecutionContext);
        return false;
    }

    @Override
    public int priority() {
        return 2;
    }
}
