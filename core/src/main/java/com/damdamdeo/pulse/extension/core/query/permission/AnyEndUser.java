package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.Objects;
import java.util.Set;

public final class AnyEndUser implements Permission {

    public static final AnyEndUser INSTANCE = new AnyEndUser();

    @Override
    public boolean allow(final Set<AggregateId> aggregateIds, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(aggregateIds);
        Objects.requireNonNull(permissionExecutionContext);
        return permissionExecutionContext.isEndUser();
    }

    @Override
    public int priority() {
        return 1;
    }
}
