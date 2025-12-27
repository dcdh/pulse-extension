package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Target;

import java.util.Objects;

public record TargetWithSource(Target target, FromApplication fromApplication) {

    public TargetWithSource {
        Objects.requireNonNull(target);
        Objects.requireNonNull(fromApplication);
    }

    public String channel() {
        return "%s-%s-%s-in".formatted(target.name().toLowerCase(),
                fromApplication.functionalDomain().toLowerCase(),
                fromApplication.componentName().toLowerCase());
    }
}