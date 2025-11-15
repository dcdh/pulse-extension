package com.damdamdeo.pulse.extension.core.event;

import java.util.Objects;

public record TodoItemAdded(String description) implements Event {

    public TodoItemAdded {
        Objects.requireNonNull(description);
    }
}
