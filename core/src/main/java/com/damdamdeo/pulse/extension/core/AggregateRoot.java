package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public abstract class AggregateRoot<K extends AggregateId> implements Belonging, Ownership {

    protected final K id;

    protected AggregateRoot(final K id) {
        this.id = Objects.requireNonNull(id);
    }

    public K id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AggregateRoot<?> that = (AggregateRoot<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "AggregateRoot{" +
                "id=" + id +
                '}';
    }
}
