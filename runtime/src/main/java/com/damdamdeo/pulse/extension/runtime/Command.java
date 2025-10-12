package com.damdamdeo.pulse.extension.runtime;

public interface Command<K extends AggregateId<?>> {
    K id();
}
