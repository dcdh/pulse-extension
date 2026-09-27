package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class VisibilityRoleRestricted implements Permission {

    public static final VisibilityRoleRestricted INSTANCE = new VisibilityRoleRestricted();

    private VisibilityRoleRestricted() {
    }

    @Override
    public boolean allow(final Set<AggregateId> aggregateIds, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(aggregateIds);
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        final List<String> visibilityRoles = permissionExecutionContext.backendUserVisibilityRolesProvider().provide();
        return visibilityRoles.stream().anyMatch(executionContext::hasRole);
    }

    @Override
    public int priority() {
        return 2;
    }
}
