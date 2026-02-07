package com.damdamdeo.pulse.extension.core.spec;

public interface Specification<T> {
    boolean isSatisfiedBy(T t);
}
