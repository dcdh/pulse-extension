package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record NewTodoCreated(String description, Owner owner) implements Event<TodoId> {

    public NewTodoCreated {
        Objects.requireNonNull(description);
        Objects.requireNonNull(owner);
    }
}
