package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.List;
import java.util.Objects;

public final class VisibilityRoleRestricted<K extends AggregateId> implements Permission<K> {

    @Override
    public boolean allow(final K aggregateId, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        final List<String> visibilityRoles = permissionExecutionContext.backendUserVisibilityRolesProvider().provide();
        return visibilityRoles.stream().anyMatch(executionContext::hasRole);
    }
}
