package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.usecase.permission.UserValidity;

import java.util.Objects;

public final class IsUserValidePermission extends SpecificQuery<TodoId> {

    private final UserId userId;
    private final UserValidity userValidity;

    public IsUserValidePermission(final UserId userId, final UserValidity userValidity) {
        this.userId = Objects.requireNonNull(userId);
        this.userValidity = Objects.requireNonNull(userValidity);
    }

    @Override
    public boolean allow(final TodoId aggregateId, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(permissionExecutionContext);
        return userValidity.isValid(userId);
    }
}
