package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.List;
import java.util.Objects;

public final class VisibilityRoleRestricted implements Permission {

    public static final VisibilityRoleRestricted INSTANCE = new VisibilityRoleRestricted();

    private VisibilityRoleRestricted() {
    }

    @Override
    public boolean allow(final AggregateId aggregateId, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        return allow(permissionExecutionContext);
    }

    @Override
    public boolean allow(final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(permissionExecutionContext);
        final ExecutionContext executionContext = permissionExecutionContext.executionContextProvider().provide();
        final List<String> visibilityRoles = permissionExecutionContext.backendUserVisibilityRolesProvider().provide();
        return visibilityRoles.stream().anyMatch(executionContext::hasRole);
    }

    @Override
    public int priority() {
        return 1;
    }
}
