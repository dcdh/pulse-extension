package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.featureflag.Feature;

import java.util.Objects;

public final class TraceabilityFeature implements Feature {

    private final ExecutionContextProvider executionContextProvider;
    private final TraceabilityRoleProvider traceabilityRoleProvider;

    public static final String TRACEABILITY = "TRACEABILITY";

    public TraceabilityFeature(final ExecutionContextProvider executionContextProvider,
                               final TraceabilityRoleProvider traceabilityRoleProvider) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.traceabilityRoleProvider = Objects.requireNonNull(traceabilityRoleProvider);
    }

    @Override
    public String name() {
        return TRACEABILITY;
    }

    @Override
    public boolean isEnabled() {
        return executionContextProvider.provide().hasRole(traceabilityRoleProvider.provide());
    }
}
