package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.Arrays;
import java.util.Objects;

public record ExecutedByHasAtLeastOneRole<K extends AggregateId>(String... roleNames) implements CompositePermission<K> {

    public ExecutedByHasAtLeastOneRole {
        Objects.requireNonNull(roleNames);
    }

    @Override
    public boolean allow(final K aggregateId, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        return Arrays.stream(roleNames).anyMatch(executionContext::hasRole);
    }
}
