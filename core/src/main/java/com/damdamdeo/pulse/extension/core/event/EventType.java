package com.damdamdeo.pulse.extension.core.event;

import java.util.Objects;

public record EventType(String type) {
    public EventType {
        Objects.requireNonNull(type);
    }

    public static <A extends Event> EventType from(Class<A> clazz) {
        return new EventType(clazz.getName());
    }
}
