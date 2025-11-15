package com.damdamdeo.pulse.extension.core;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record AggregateRootType(String type) {

    public AggregateRootType {
        Objects.requireNonNull(type);
        Validate.isTrue(!type.contains("."));
    }

    public static <A extends AggregateRoot<?>> AggregateRootType from(Class<A> clazz) {
        return new AggregateRootType(clazz.getSimpleName());
    }
}
