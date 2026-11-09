package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;

import java.util.Objects;

public sealed interface CompositePermission<A extends AggregateId> extends Permission<A>
        permits OrPermission, AndPermission, Everyone, AnyEndUser, VisibilityRoleRestricted, InExecutedBy,
        ExecutedBySpecificServiceAccounts, ExecutedByHasAtLeastOneRole, ExecutedBySpecificEndUsers, SpecificQuery,
        ExecutedBySpecificJob {

    default CompositePermission<A> or(final Permission<A>... permissions) {
        Objects.requireNonNull(permissions);
        return new OrPermission<>(this, permissions);
    }

    default CompositePermission<A> and(final Permission<A>... permissions) {
        Objects.requireNonNull(permissions);
        return new AndPermission<>(this, permissions);
    }
}
