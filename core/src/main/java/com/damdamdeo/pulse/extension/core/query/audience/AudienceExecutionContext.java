package com.damdamdeo.pulse.extension.core.query.audience;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.AggregateIdDecomposer;
import com.damdamdeo.pulse.extension.core.query.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.query.ExecutedByResolver;

import java.util.Objects;

public record AudienceExecutionContext(ExecutionContextProvider executionContextProvider,
                                       BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                                       ExecutedByResolver executedByResolver,
                                       AggregateIdDecomposer aggregateIdDecomposer) {

    public AudienceExecutionContext {
        Objects.requireNonNull(executionContextProvider);
        Objects.requireNonNull(backendUserVisibilityRolesProvider);
        Objects.requireNonNull(executedByResolver);
        Objects.requireNonNull(aggregateIdDecomposer);
    }
}
