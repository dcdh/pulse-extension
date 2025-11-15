package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateVersion;

import java.util.Objects;

public record VersionizedEvent(AggregateVersion version, Event event) {

    public VersionizedEvent {
        Objects.requireNonNull(version);
        Objects.requireNonNull(event);
    }
}
