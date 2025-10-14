package com.damdamdeo.pulse.extension.core;

public interface AggregateRoot<K extends AggregateId> {
    K id();
}
