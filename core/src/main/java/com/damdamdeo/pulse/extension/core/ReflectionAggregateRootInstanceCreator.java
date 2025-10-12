package com.damdamdeo.pulse.extension.core;

final class ReflectionAggregateRootInstanceCreator implements AggregateRootInstanceCreator {

    @Override
    public <A extends AggregateRoot<?>> A create(final Class<A> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
