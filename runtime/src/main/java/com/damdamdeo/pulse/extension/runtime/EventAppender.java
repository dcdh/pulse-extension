package com.damdamdeo.pulse.extension.runtime;

public interface EventAppender<K extends AggregateId<?>> {
    void append(Event<K> event);
}
