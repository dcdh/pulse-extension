package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.ExecutionContext;

public interface Specification<T> {
    boolean isSatisfiedBy(T t, ExecutionContext executionContext);
}
