package com.damdamdeo.pulse.extension.core.usecase.audience;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.Objects;
import java.util.stream.Stream;

public record ExecutedBySpecificEndUsers(ExecutedBy.EndUser... endUsers) implements Audience {

    public ExecutedBySpecificEndUsers {
        Objects.requireNonNull(endUsers);
    }

    @Override
    public boolean allow(final AggregateId aggregateId, final AudienceExecutionContext audienceExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(audienceExecutionContext);
        return allow(audienceExecutionContext);
    }

    @Override
    public boolean allow(final AudienceExecutionContext audienceExecutionContext) throws UseCaseException {
        Objects.requireNonNull(audienceExecutionContext);
        final ExecutionContext executionContext = audienceExecutionContext.executionContextProvider().provide();
        final ExecutedBy executedBy = executionContext.executedBy();
        return Stream.of(endUsers).anyMatch(endUser -> endUser.value().equals(executedBy.value()));
    }

    @Override
    public int priority() {
        return 5;
    }
}
