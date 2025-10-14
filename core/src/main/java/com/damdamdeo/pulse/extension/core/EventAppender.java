package com.damdamdeo.pulse.extension.core;

public interface EventAppender<K extends AggregateId> {
    void append(Event<K> event);
}
