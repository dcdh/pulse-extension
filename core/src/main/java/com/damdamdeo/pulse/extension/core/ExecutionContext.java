package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Objects;
import java.util.Set;

public record ExecutionContext(ExecutedBy executedBy, Set<String> roles) {

    public ExecutionContext {
        Objects.requireNonNull(executedBy);
        Objects.requireNonNull(roles);
    }

    public boolean hasRole(final String role) {
        Objects.requireNonNull(role);
        return roles.contains(role);
    }
}
