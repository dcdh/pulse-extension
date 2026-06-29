package com.damdamdeo.pulse.extension.core.spec;

import java.util.Objects;

public interface CompositeSpecification<T> extends Specification<T> {

    default CompositeSpecification<T> or(final Specification<T>... specifications) {
        Objects.requireNonNull(specifications);
        return new OrSpecification<>(this, specifications);
    }

    default CompositeSpecification<T> and(final Specification<T>... specifications) {
        Objects.requireNonNull(specifications);
        return new AndSpecification<>(this, specifications);
    }

    default CompositeSpecification<T> not() {
        return new NotSpecification<>(this);
    }
}
