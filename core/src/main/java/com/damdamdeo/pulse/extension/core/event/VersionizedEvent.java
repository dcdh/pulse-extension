package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateVersion;

import java.util.Objects;

public record VersionizedEvent<K extends AggregateId>(AggregateVersion version, Event<K> event) {

    public VersionizedEvent {
        Objects.requireNonNull(version);
        Objects.requireNonNull(event);
    }
}
