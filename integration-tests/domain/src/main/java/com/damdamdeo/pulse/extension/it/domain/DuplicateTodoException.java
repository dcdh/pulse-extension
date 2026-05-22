package com.damdamdeo.pulse.extension.it.domain;

import com.damdamdeo.pulse.extension.core.DuplicateAggregateException;
import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Objects;

public class DuplicateTodoException extends DuplicateAggregateException {

    private final TodoId todoId;

    public DuplicateTodoException(final TodoId todoId) {
        this.todoId = Objects.requireNonNull(todoId);
    }
}
