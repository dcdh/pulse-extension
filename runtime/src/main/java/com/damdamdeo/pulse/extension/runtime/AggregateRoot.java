package com.damdamdeo.pulse.extension.runtime;

public interface AggregateRoot<K extends AggregateId<?>> {
    K id();
}
