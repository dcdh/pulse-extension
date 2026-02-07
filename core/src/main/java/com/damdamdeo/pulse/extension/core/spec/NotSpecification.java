package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.ExecutionContext;

import java.util.Objects;

public final class NotSpecification<T> extends CompositeSpecification<T> {

    private final Specification<T> specification;

    public NotSpecification(final Specification<T> specification) {
        this.specification = Objects.requireNonNull(specification);
    }

    @Override
    public boolean isSatisfiedBy(final T t, final ExecutionContext executionContext) {
        Objects.requireNonNull(t);
        Objects.requireNonNull(executionContext);
        return !specification.isSatisfiedBy(t, executionContext);
    }
}
