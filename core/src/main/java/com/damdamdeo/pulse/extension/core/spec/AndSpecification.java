package com.damdamdeo.pulse.extension.core.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AndSpecification<T> extends CompositeSpecification<T> {

    private final List<Specification<T>> specifications;

    @SafeVarargs
    public AndSpecification(final Specification<T> first, final Specification<T>... others) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(others);

        this.specifications = new ArrayList<>(others.length + 1);
        specifications.add(first);
        specifications.addAll(List.of(others));
    }

    public boolean isSatisfiedBy(final T t) {
        Objects.requireNonNull(t);
        return specifications.stream().allMatch(specification -> specification.isSatisfiedBy(t));
    }
}
