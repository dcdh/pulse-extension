package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public record ExecutedByHasAtLeastOneRole(String... roleNames) implements Permission {

    public ExecutedByHasAtLeastOneRole {
        Objects.requireNonNull(roleNames);
    }

    @Override
    public boolean allow(final Set<AggregateId> aggregateIds, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(aggregateIds);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        return Arrays.stream(roleNames).anyMatch(executionContext::hasRole);
    }

    @Override
    public int priority() {
        return 5;
    }
}
