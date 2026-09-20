package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.Objects;

public final class Everyone implements Permission {

    public static final Everyone INSTANCE = new Everyone();

    private Everyone() {
    }

    @Override
    public boolean allow(final AggregateId aggregateId, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(permissionExecutionContext);
        return allow(permissionExecutionContext);
    }

    @Override
    public boolean allow(final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(permissionExecutionContext);
        return true;
    }

    @Override
    public int priority() {
        return 0;
    }
}
