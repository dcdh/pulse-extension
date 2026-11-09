package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.Arrays;
import java.util.Objects;

public record ExecutedByHasAtLeastOneRole<A extends AggregateId>(
        String... roleNames) implements CompositePermission<A> {

    public ExecutedByHasAtLeastOneRole {
        Objects.requireNonNull(roleNames);
    }

    @Override
    public boolean allow(final ProjectionId<A> projectionId, final PermissionExecutionContext permissionExecutionContext)
            throws QueryException {
        Objects.requireNonNull(projectionId);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        return Arrays.stream(roleNames).anyMatch(executionContext::hasRole);
    }
}
