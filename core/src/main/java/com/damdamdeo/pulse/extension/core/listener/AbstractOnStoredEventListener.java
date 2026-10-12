package com.damdamdeo.pulse.extension.core.listener;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.Event;

import java.util.Objects;

public abstract class AbstractOnStoredEventListener<K extends AggregateId, E extends Event<K>> implements OnStoredEventListener<K, E> {

    @Override
    public final void execute(K id, Event<K> event) throws OnStoredEventListenerException {
        Objects.requireNonNull(id);
        Objects.requireNonNull(event);
        if (eventType().isInstance(event)) {
            on(id, eventType().cast(event));
        }
    }

    protected abstract Class<E> eventType();
}
