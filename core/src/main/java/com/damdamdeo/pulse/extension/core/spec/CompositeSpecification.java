package com.damdamdeo.pulse.extension.core.spec;

import java.util.Objects;

public abstract class CompositeSpecification<T> implements Specification<T> {

    public CompositeSpecification<T> or(final Specification<T>... specifications) {
        Objects.requireNonNull(specifications);
        return new OrSpecification<>(this, specifications);
    }

    public CompositeSpecification<T> and(final Specification<T>... specifications) {
        Objects.requireNonNull(specifications);
        return new AndSpecification<>(this, specifications);
    }

    public CompositeSpecification<T> not() {
        return new NotSpecification<>(this);
    }
}
