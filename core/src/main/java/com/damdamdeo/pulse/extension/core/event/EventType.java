package com.damdamdeo.pulse.extension.core.event;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record EventType(String type) {
    public EventType {
        Objects.requireNonNull(type);
        Validate.isTrue(!type.contains("."));
    }

    public static <A extends Event> EventType from(Class<A> clazz) {
        return new EventType(clazz.getSimpleName());
    }
}
