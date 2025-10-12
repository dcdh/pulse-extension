package com.damdamdeo.pulse.extension.core;

public interface AggregateRootInstanceCreator {
    <A extends AggregateRoot<?>> A create(Class<A> clazz);
}
