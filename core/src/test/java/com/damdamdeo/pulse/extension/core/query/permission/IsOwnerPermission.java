package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.OwnerQuery;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.Objects;

public final class IsOwnerPermission extends SpecificQuery<TodoId> {

    private final UserId userId;
    private final OwnerQuery ownerQuery;

    public IsOwnerPermission(final UserId userId, final OwnerQuery ownerQuery) {
        this.userId = Objects.requireNonNull(userId);
        this.ownerQuery = Objects.requireNonNull(ownerQuery);
    }

    @Override
    public boolean allow(final TodoId aggregateId, final PermissionExecutionContext permissionExecutionContext)
            throws QueryException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(permissionExecutionContext);
        return ownerQuery.getByTodoId(aggregateId).id().equals(userId);
    }
}
