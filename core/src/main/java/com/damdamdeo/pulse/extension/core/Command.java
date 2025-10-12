package com.damdamdeo.pulse.extension.core;

public interface Command<K extends AggregateId<?>> {
    K id();
}
