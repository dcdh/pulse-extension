package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.Arrays;
import java.util.Objects;

public record ExecutedByHasAtLeastOneRole(String... roleNames) implements Permission {

    public ExecutedByHasAtLeastOneRole {
        Objects.requireNonNull(roleNames);
    }

    @Override
    public boolean allow(final AggregateId aggregateId, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(permissionExecutionContext);
        return allow(permissionExecutionContext);
    }

    @Override
    public boolean allow(PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        return Arrays.stream(roleNames).anyMatch(executionContext::hasRole);
    }

    @Override
    public int priority() {
        return 5;
    }
}
