package com.damdamdeo.pulse.extension.core.event;

import java.util.Objects;

public record EventType(String type) {
    public EventType {
        Objects.requireNonNull(type);
    }
}
