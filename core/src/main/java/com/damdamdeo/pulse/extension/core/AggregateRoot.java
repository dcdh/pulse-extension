package com.damdamdeo.pulse.extension.core;

public interface AggregateRoot<K extends AggregateId> extends Belonging, Ownership {
    K id();
}
