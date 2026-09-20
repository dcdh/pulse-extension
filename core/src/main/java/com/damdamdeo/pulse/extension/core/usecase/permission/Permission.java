package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.Prioritable;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

public sealed interface Permission<K extends AggregateId, C extends Command<K>> extends Prioritable permits Everyone, AnyEndUser, VisibilityRoleRestricted, InExecutedBy, ExecutedBySpecificServiceAccounts,
        ExecutedByHasAtLeastOneRole, ExecutedBySpecificEndUsers, SpecificDomain {

    boolean allow(K aggregateId, C command, PermissionExecutionContext permissionExecutionContext) throws UseCaseException;

    boolean allow(C command, PermissionExecutionContext permissionExecutionContext) throws UseCaseException;
}
