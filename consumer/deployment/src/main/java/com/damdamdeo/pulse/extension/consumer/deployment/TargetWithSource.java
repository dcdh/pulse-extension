package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import com.damdamdeo.pulse.extension.core.consumer.Table;

import java.util.Objects;

public record TargetWithSource(Purpose purpose, FromApplication fromApplication) {

    public TargetWithSource {
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(fromApplication);
    }

    public String channel(final Table table) {
        Objects.requireNonNull(table);
        return "%s-%s-%s-%s-in".formatted(purpose.name().toLowerCase(),
                fromApplication.functionalDomain().toLowerCase(),
                fromApplication.componentName().toLowerCase(),
                table.name().replace("_", "-").toLowerCase());
    }
}
