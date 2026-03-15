package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateVersion;

import java.util.Objects;

public record VersionizedEvent(AggregateVersion version, ExecutedByEvent executedByEvent) {

    public VersionizedEvent {
        Objects.requireNonNull(version);
        Objects.requireNonNull(executedByEvent);
    }

    public Event event() {
        return executedByEvent.event();
    }
}
