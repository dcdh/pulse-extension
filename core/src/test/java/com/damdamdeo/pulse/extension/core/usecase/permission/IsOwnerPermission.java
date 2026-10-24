package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.OwnerQuery;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.Objects;

public final class IsOwnerPermission<C extends Command<TodoId>> extends SpecificDomain<TodoId, C> {

    private final UserId userId;
    private final OwnerQuery ownerQuery;

    public IsOwnerPermission(final UserId userId, final OwnerQuery ownerQuery) {
        this.userId = Objects.requireNonNull(userId);
        this.ownerQuery = Objects.requireNonNull(ownerQuery);
    }

    @Override
    public boolean allow(final TodoId aggregateId, final C command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(command);
        Objects.requireNonNull(permissionExecutionContext);
        return ownerQuery.getByTodoId(aggregateId).id().equals(userId);
    }

    @Override
    public boolean allow(final C command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(permissionExecutionContext);
        return ownerQuery.getByTodoId(command.id()).id().equals(userId);
    }
}
