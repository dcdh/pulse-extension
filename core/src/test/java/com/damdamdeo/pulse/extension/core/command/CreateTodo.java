package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record CreateTodo(Description description, Owner owner) implements CreationalCommand<TodoId> {

    public CreateTodo {
        Objects.requireNonNull(description);
        Objects.requireNonNull(owner);
    }
}
