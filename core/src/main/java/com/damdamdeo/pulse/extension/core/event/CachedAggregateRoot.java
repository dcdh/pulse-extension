package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.AggregateVersion;

import java.util.Objects;

public record CachedAggregateRoot<A extends AggregateRoot<?>>(A aggregateRoot, AggregateVersion aggregateVersion) {

    public CachedAggregateRoot {
        Objects.requireNonNull(aggregateRoot);
        Objects.requireNonNull(aggregateVersion);
    }
}
