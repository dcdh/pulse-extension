package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.Objects;

public final class IsUserValidePermission<C extends Command<TodoId>> extends SpecificDomain<TodoId, C> {

    private final UserId userId;
    private final UserValidity userValidity;

    public IsUserValidePermission(final UserId userId, final UserValidity userValidity) {
        this.userId = Objects.requireNonNull(userId);
        this.userValidity = Objects.requireNonNull(userValidity);
    }

    @Override
    public boolean allow(final TodoId aggregateId, final C command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(command);
        Objects.requireNonNull(permissionExecutionContext);
        return userValidity.isValid(userId);
    }

    @Override
    public boolean allow(final C command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(permissionExecutionContext);
        return userValidity.isValid(userId);
    }
}
