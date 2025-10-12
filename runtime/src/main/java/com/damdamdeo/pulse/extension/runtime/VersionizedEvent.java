package com.damdamdeo.pulse.extension.runtime;

import java.util.Objects;

public record VersionizedEvent<K extends AggregateId<?>>(AggregateVersion version, Event<K> event) {

    public VersionizedEvent {
        Objects.requireNonNull(version);
        Objects.requireNonNull(event);
    }
}
