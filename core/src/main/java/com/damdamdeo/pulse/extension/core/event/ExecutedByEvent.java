package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Objects;

public record ExecutedByEvent<K extends AggregateId>(Event<K> event, ExecutedBy executedBy) {

    public ExecutedByEvent {
        Objects.requireNonNull(event);
        Objects.requireNonNull(executedBy);
    }
}
