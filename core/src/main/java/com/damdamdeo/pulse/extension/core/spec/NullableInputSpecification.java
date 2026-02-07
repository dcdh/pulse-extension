package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.ExecutionContext;

public final class NullableInputSpecification<T> implements Specification<T> {

    @Override
    public boolean isSatisfiedBy(final T t, final ExecutionContext executionContext) {
        return t != null;
    }
}
