package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.Prioritable;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

public sealed interface Permission extends Prioritable permits Everyone, AnyEndUser, VisibilityRoleRestricted, InExecutedBy, ExecutedBySpecificServiceAccounts,
        ExecutedByHasAtLeastOneRole, ExecutedBySpecificEndUsers {

    boolean allow(AggregateId aggregateId, PermissionExecutionContext permissionExecutionContext) throws UseCaseException;

    boolean allow(PermissionExecutionContext permissionExecutionContext) throws UseCaseException;
}
