package com.damdamdeo.pulse.extension.runtime;

public interface AggregateRootInstanceCreator {
    <A extends AggregateRoot<?>> A create(Class<A> clazz);
}
