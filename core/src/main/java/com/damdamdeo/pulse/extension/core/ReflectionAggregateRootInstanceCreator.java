package com.damdamdeo.pulse.extension.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;

public final class ReflectionAggregateRootInstanceCreator implements AggregateRootInstanceCreator {

    @Override
    public <A extends AggregateRoot<K>, K extends AggregateId> A create(final Class<A> aggregateRootClazz,
                                                                      final Class<K> aggregateIdClass,
                                                                      final K aggregateId) {
        Objects.requireNonNull(aggregateRootClazz);
        Objects.requireNonNull(aggregateIdClass);
        Objects.requireNonNull(aggregateId);
        try {
            // Keep existing ID-only aggregates compatible during migration.
            try {
                return aggregateRootClazz.getConstructor(aggregateIdClass).newInstance(aggregateId);
            } catch (final NoSuchMethodException ignored) {
                // A full-state constructor receives an empty state before event replay.
                final Constructor<?>[] constructors = Arrays.stream(aggregateRootClazz.getConstructors())
                        .filter(constructor -> constructor.getParameterCount() > 1)
                        .filter(constructor -> constructor.getParameterTypes()[0].equals(aggregateIdClass))
                        .toArray(Constructor<?>[]::new);
                if (constructors.length != 1) {
                    throw new IllegalArgumentException("Expected a single full-state constructor for "
                            + aggregateRootClazz.getName());
                }
                final Constructor<?> constructor = constructors[0];
                if (Arrays.stream(constructor.getParameterTypes()).anyMatch(Class::isPrimitive)) {
                    throw new IllegalArgumentException("Replay constructor state must use reference types: "
                            + aggregateRootClazz.getName());
                }
                final Object[] arguments = new Object[constructor.getParameterCount()];
                arguments[0] = aggregateId;
                return aggregateRootClazz.cast(constructor.newInstance(arguments));
            }
        } catch (final InvocationTargetException exception) {
            throw new IllegalStateException("Cannot initialize aggregate before event replay: "
                    + aggregateRootClazz.getName(), exception.getCause());
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot initialize aggregate: " + aggregateRootClazz.getName(), exception);
        }
    }
}
