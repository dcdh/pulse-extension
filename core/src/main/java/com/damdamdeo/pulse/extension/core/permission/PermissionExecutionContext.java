package com.damdamdeo.pulse.extension.core.permission;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.AggregateIdDecomposer;

import java.util.Objects;

public record PermissionExecutionContext(ExecutionContextProvider executionContextProvider,
                                         BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                                         ExecutedByResolver executedByResolver,
                                         AggregateIdDecomposer aggregateIdDecomposer) {

    public PermissionExecutionContext {
        Objects.requireNonNull(executionContextProvider);
        Objects.requireNonNull(backendUserVisibilityRolesProvider);
        Objects.requireNonNull(executedByResolver);
        Objects.requireNonNull(aggregateIdDecomposer);
    }

    public boolean isEndUser() {
        return executionContextProvider().provide().executedBy().isEndUser();
    }
}
