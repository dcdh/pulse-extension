package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Objects;

public record ExecutedByEvent(Event event, ExecutedBy executedBy) {

    public ExecutedByEvent {
        Objects.requireNonNull(event);
        Objects.requireNonNull(executedBy);
    }
}
