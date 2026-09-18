package com.damdamdeo.pulse.extension.obfuscator;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public record Todo(TodoId todoId, String description, Status status, Boolean important) {

    public Todo {
        Objects.requireNonNull(todoId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(status);
        Objects.requireNonNull(important);
    }
}
