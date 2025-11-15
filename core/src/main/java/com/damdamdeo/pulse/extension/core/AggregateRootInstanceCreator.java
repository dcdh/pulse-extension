package com.damdamdeo.pulse.extension.core;

public interface AggregateRootInstanceCreator {
    <A extends AggregateRoot<K>, K extends AggregateId> A create(Class<A> aggregateRootClazz,
                                                                 Class<K> aggregateIdClass,
                                                                 K aggregateId);
}
