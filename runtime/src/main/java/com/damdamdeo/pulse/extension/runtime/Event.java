package com.damdamdeo.pulse.extension.runtime;

public interface Event<K extends AggregateId<?>> {
    K id();
}
