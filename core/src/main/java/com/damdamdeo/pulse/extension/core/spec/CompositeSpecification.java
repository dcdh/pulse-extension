package com.damdamdeo.pulse.extension.core.spec;

import java.util.Objects;

public abstract class CompositeSpecification<T> implements Specification<T> {

    public CompositeSpecification<T> or(final Specification<T> specification) {
        Objects.requireNonNull(specification);
        return new OrSpecification<>(this, specification);
    }

    public CompositeSpecification<T> and(final Specification<T> specification) {
        Objects.requireNonNull(specification);
        return new AndSpecification<>(this, specification);
    }

    public CompositeSpecification<T> not() {
        return new NotSpecification<>(this);
    }
}
