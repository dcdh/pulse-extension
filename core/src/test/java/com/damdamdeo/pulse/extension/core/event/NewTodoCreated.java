package com.damdamdeo.pulse.extension.core.event;

import java.util.Objects;

public record NewTodoCreated(String description) implements Event {

    public NewTodoCreated {
        Objects.requireNonNull(description);
    }
}
