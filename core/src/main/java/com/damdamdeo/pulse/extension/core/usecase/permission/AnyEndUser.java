package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.Objects;

public final class AnyEndUser<K extends AggregateId, C extends Command<K>> implements Permission<K, C> {

    @Override
    public boolean allow(final K aggregateId, final C command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(command);
        Objects.requireNonNull(permissionExecutionContext);
        return allow(command, permissionExecutionContext);
    }

    @Override
    public boolean allow(final C command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(permissionExecutionContext);
        return permissionExecutionContext.isEndUser();
    }

    @Override
    public int priority() {
        return 1;
    }
}
