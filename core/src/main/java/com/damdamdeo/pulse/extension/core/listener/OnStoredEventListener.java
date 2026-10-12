package com.damdamdeo.pulse.extension.core.listener;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.Event;

public interface OnStoredEventListener<K extends AggregateId, E extends Event<K>> {

    void on(K id, E event) throws OnStoredEventListenerException;

    void execute(K id, Event<K> event) throws OnStoredEventListenerException;
}
