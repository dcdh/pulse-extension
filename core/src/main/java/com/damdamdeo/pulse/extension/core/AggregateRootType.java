package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record AggregateRootType(String type) {

    public AggregateRootType {
        Objects.requireNonNull(type);
    }

    public static <A extends AggregateRoot<?>> AggregateRootType from(Class<A> clazz) {
        return new AggregateRootType(clazz.getName());
    }
}
