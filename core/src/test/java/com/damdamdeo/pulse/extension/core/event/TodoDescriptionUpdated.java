package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record TodoDescriptionUpdated(String description) implements Event<TodoId> {

    public TodoDescriptionUpdated {
        Objects.requireNonNull(description);
    }
}
