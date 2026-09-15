package com.damdamdeo.pulse.extension.core.usecase.audience;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.Arrays;
import java.util.Objects;

public record ExecutedByHasAtLeastOneRole(String... roleNames) implements Audience {

    public ExecutedByHasAtLeastOneRole {
        Objects.requireNonNull(roleNames);
    }

    @Override
    public boolean allow(final AggregateId aggregateId, final AudienceExecutionContext audienceExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(audienceExecutionContext);
        return allow(audienceExecutionContext);
    }

    @Override
    public boolean allow(AudienceExecutionContext audienceExecutionContext) throws UseCaseException {
        Objects.requireNonNull(audienceExecutionContext);
        final ExecutionContext executionContext = audienceExecutionContext.executionContextProvider().provide();
        return Arrays.stream(roleNames).anyMatch(executionContext::hasRole);
    }

    @Override
    public int priority() {
        return 4;
    }
}
