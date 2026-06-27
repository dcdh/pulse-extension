package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import com.damdamdeo.pulse.extension.core.consumer.Table;

import java.util.Objects;
import java.util.stream.Collectors;

public record TargetWithSource(Purpose purpose, FromApplication fromApplication) {

    public TargetWithSource {
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(fromApplication);
    }

    public String channel(final Table table) {
        Objects.requireNonNull(table);
        return "%s-%s-%s-in".formatted(purpose.name().toLowerCase(),
                fromApplication.applicationNaming().split().stream().map(String::toLowerCase)
                        .collect(Collectors.joining("-")),
                table.name().replace("_", "-").toLowerCase());
    }
}
