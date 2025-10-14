package com.damdamdeo.pulse.extension.core;

public interface Event<K extends AggregateId> {
    K id();
}
