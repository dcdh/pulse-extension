package com.damdamdeo.pulse.extension.core.spec;

public final class NullableInputSpecification<T> implements Specification<T> {

    @Override
    public boolean isSatisfiedBy(T t) {
        return t != null;
    }
}
