package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.featureflag.Feature;

import java.util.Objects;

import static com.damdamdeo.pulse.extension.core.traceability.Finder.ROLE_TRACEABILITY_READ;

public final class TraceabilityFeature implements Feature {

    private final ExecutionContextProvider executionContextProvider;

    public static final String TRACEABILITY = "TRACEABILITY";

    public TraceabilityFeature(final ExecutionContextProvider executionContextProvider) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
    }

    @Override
    public String name() {
        return TRACEABILITY;
    }

    @Override
    public boolean isEnabled() {
        return executionContextProvider.provide().hasRole(ROLE_TRACEABILITY_READ);
    }
}
