package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record VersionizedAggregateRoot<A extends AggregateRoot<?>>(A aggregateRoot, AggregateVersion aggregateVersion) {

    public VersionizedAggregateRoot {
        Objects.requireNonNull(aggregateRoot);
        Objects.requireNonNull(aggregateVersion);
    }
}
