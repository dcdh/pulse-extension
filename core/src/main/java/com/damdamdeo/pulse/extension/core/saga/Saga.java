package com.damdamdeo.pulse.extension.core.saga;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.Event;

import java.util.Objects;

public interface Saga<K extends AggregateId, E extends Event<K>> {

    void on(K id, E event);

    default void execute(K id, Event<K> event) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(event);
        if (eventType().isInstance(event)) {
            on(id, eventType().cast(event));
        }
    }

    Class<E> eventType();
}
