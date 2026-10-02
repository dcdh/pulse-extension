package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;

import java.util.List;
import java.util.Objects;

public record Handled<A extends AggregateRoot<K>, K extends AggregateId>(A aggregateRoot,
                                                                         List<VersionizedEvent<K>> events) {

    public Handled {
        Objects.requireNonNull(aggregateRoot);
        Objects.requireNonNull(events);
    }

    public K id() {
        return aggregateRoot.id();
    }
}
