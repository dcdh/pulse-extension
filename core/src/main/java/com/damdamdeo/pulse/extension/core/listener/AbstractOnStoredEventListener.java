package com.damdamdeo.pulse.extension.core.listener;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.event.Event;

import java.util.Objects;

public abstract class AbstractOnStoredEventListener<K extends AggregateId, E extends Event<K>> implements OnStoredEventListener<K, E> {

    @Override
    public final void execute(K id, final AggregateVersion aggregateVersion, Event<K> event) throws OnStoredEventListenerException {
        Objects.requireNonNull(id);
        Objects.requireNonNull(aggregateVersion);
        Objects.requireNonNull(event);
        if (eventType().isInstance(event)) {
            on(id, aggregateVersion, eventType().cast(event));
        }
    }

    protected abstract Class<E> eventType();
}
