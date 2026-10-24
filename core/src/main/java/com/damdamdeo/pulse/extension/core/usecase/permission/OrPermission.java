package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OrPermission<K extends AggregateId, C extends Command<K>> implements CompositePermission<K, C> {

    private final List<Permission<K, C>> Permissions;

    @SafeVarargs
    public OrPermission(final Permission<K, C> first, final Permission<K, C>... others) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(others);
        this.Permissions = new ArrayList<>(others.length + 1);
        Permissions.add(first);
        Permissions.addAll(List.of(others));
    }

    @Override
    public boolean allow(final K aggregateId, final C command,
                         final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(command);
        Objects.requireNonNull(permissionExecutionContext);
        for (final Permission<K, C> Permission : Permissions) {
            if (Permission.allow(aggregateId, command, permissionExecutionContext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allow(final C command, final PermissionExecutionContext permissionExecutionContext)
            throws UseCaseException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(permissionExecutionContext);
        for (final Permission<K, C> Permission : Permissions) {
            if (Permission.allow(command, permissionExecutionContext)) {
                return true;
            }
        }
        return false;
    }
}
