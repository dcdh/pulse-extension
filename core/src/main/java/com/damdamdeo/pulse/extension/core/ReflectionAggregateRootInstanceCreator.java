package com.damdamdeo.pulse.extension.core;

public final class ReflectionAggregateRootInstanceCreator implements AggregateRootInstanceCreator {

    @Override
    public <A extends AggregateRoot<K>, K extends AggregateId> A create(final Class<A> aggregateRootClazz,
                                                                        final Class<K> aggregateIdClass,
                                                                        final K aggregateId) {
        try {
            return aggregateRootClazz.getDeclaredConstructor(aggregateIdClass).newInstance(aggregateId);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
