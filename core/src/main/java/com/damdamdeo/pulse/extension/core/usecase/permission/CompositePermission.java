package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;

import java.util.Objects;

public sealed interface CompositePermission<K extends AggregateId, C extends Command<K>> extends Permission<K, C>
        permits SpecificDomain, OrPermission, AndPermission, Everyone, AnyEndUser, VisibilityRoleRestricted, InExecutedBy, ExecutedBySpecificServiceAccounts,
        ExecutedByHasAtLeastOneRole, ExecutedBySpecificEndUsers {

    default CompositePermission<K, C> or(final Permission<K, C>... permissions) {
        Objects.requireNonNull(permissions);
        return new OrPermission<>(this, permissions);
    }

    default CompositePermission<K, C> and(final Permission<K, C>... permissions) {
        Objects.requireNonNull(permissions);
        return new AndPermission<>(this, permissions);
    }
}
