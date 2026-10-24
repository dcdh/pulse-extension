package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OrPermission<K extends AggregateId> implements CompositePermission<K> {

    private final List<Permission<K>> Permissions;

    @SafeVarargs
    public OrPermission(final Permission<K> first, final Permission<K>... others) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(others);
        this.Permissions = new ArrayList<>(others.length + 1);
        Permissions.add(first);
        Permissions.addAll(List.of(others));
    }

    @Override
    public boolean allow(final K aggregateId, final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(permissionExecutionContext);
        for (final Permission<K> Permission : Permissions) {
            if (Permission.allow(aggregateId, permissionExecutionContext)) {
                return true;
            }
        }
        return false;
    }
}
