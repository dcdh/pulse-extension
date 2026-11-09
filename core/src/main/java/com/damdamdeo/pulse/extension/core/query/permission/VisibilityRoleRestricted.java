package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.List;
import java.util.Objects;

public final class VisibilityRoleRestricted<A extends AggregateId> implements CompositePermission<A> {

    @Override
    public boolean allow(final ProjectionId<A> projectionId, final PermissionExecutionContext permissionExecutionContext)
            throws QueryException {
        Objects.requireNonNull(projectionId);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        final List<String> visibilityRoles = permissionExecutionContext.backendUserVisibilityRolesProvider().provide();
        return visibilityRoles.stream().anyMatch(executionContext::hasRole);
    }
}
