package com.damdamdeo.pulse.extension.core.consumer;

import java.util.Objects;

public record CdcTopicNaming(FromApplication fromApplication, Table table) {

    public CdcTopicNaming {
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(table);
    }

    public String name() {
        final String schema = "%s_%s".formatted(
                fromApplication.functionalDomain().toLowerCase(),
                fromApplication.componentName().toLowerCase());
        return "pulse.%s.%s".formatted(schema, table.name().toLowerCase());
    }
}
