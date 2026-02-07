package com.damdamdeo.pulse.extension.core.spec;

import java.util.Objects;

public class OrSpecification<T> extends CompositeSpecification<T> implements Specification<T> {

    private final Specification<T> left;
    private final Specification<T> right;

    public OrSpecification(final Specification<T> pLeft, final Specification<T> pRight) {
        this.left = Objects.requireNonNull(pLeft);
        this.right = Objects.requireNonNull(pRight);
    }

    public boolean isSatisfiedBy(final T t) {
        Objects.requireNonNull(t);
        return left.isSatisfiedBy(t) || right.isSatisfiedBy(t);
    }
}
