package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.traceability.TraceabilityRoleProvider;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@Unremovable
public class SmallryeTraceabilityRoleProvider implements TraceabilityRoleProvider {

    private final TraceabilityConfiguration traceabilityConfiguration;

    public SmallryeTraceabilityRoleProvider(final TraceabilityConfiguration traceabilityConfiguration) {
        this.traceabilityConfiguration = Objects.requireNonNull(traceabilityConfiguration);
    }

    @Override
    public String provide() {
        return traceabilityConfiguration.role();
    }
}
