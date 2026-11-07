package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;

public sealed interface CompositePermission<K extends Identifiable> extends Permission<K>
        permits OrPermission, AndPermission, Everyone, AnyEndUser, VisibilityRoleRestricted, InExecutedBy,
        ExecutedBySpecificServiceAccounts, ExecutedByHasAtLeastOneRole, ExecutedBySpecificEndUsers, SpecificQuery,
        ExecutedBySpecificJob {

    default CompositePermission<K> or(final Permission<K>... permissions) {
        Objects.requireNonNull(permissions);
        return new OrPermission<>(this, permissions);
    }

    default CompositePermission<K> and(final Permission<K>... permissions) {
        Objects.requireNonNull(permissions);
        return new AndPermission<>(this, permissions);
    }
}
