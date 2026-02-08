package com.damdamdeo.pulse.extension.core.event;

import java.util.Objects;
import java.util.function.Consumer;

public record IdentifiableEvent(String id, Event event) {

    public IdentifiableEvent {
        Objects.requireNonNull(id);
        Objects.requireNonNull(event);
    }

    public <T extends Event> void executeOn(final Class<T> clazz, final Consumer<T> onMatch) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(onMatch);
        if (clazz.isInstance(event)) {
            onMatch.accept(clazz.cast(event));
        }
    }
}
