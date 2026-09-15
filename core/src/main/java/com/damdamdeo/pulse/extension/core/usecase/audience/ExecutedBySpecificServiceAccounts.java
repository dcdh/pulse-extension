package com.damdamdeo.pulse.extension.core.usecase.audience;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ExecutedBySpecificServiceAccounts(String... names) implements Audience {

    public ExecutedBySpecificServiceAccounts {
        Objects.requireNonNull(names);
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
        final Set<String> candidates = Stream.of(names).map(name -> ExecutedBy.ServiceAccount.DISCRIMINANT + ExecutedBy.SEPARATOR + name)
                .collect(Collectors.toSet());
        return candidates.contains(executionContext.executedBy().value());
    }

    @Override
    public int priority() {
        return 3;
    }
}
