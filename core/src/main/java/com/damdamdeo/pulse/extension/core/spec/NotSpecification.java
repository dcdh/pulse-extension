package com.damdamdeo.pulse.extension.core.spec;

import java.util.Objects;

public final class NotSpecification<T> extends CompositeSpecification<T> {

    private final Specification<T> specification;

    public NotSpecification(final Specification<T> pSpecification) {
        this.specification = Objects.requireNonNull(pSpecification);
    }

    @Override
    public boolean isSatisfiedBy(final T t) {
        Objects.requireNonNull(t);
        return !specification.isSatisfiedBy(t);
    }
}
