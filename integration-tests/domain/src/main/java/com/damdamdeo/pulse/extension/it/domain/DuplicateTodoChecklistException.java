package com.damdamdeo.pulse.extension.it.domain;

import com.damdamdeo.pulse.extension.core.DuplicateAggregateException;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;

import java.util.Objects;

public class DuplicateTodoChecklistException extends DuplicateAggregateException {

    private final TodoChecklistId todoChecklistId;

    public DuplicateTodoChecklistException(final TodoChecklistId todoChecklistId) {
        this.todoChecklistId = Objects.requireNonNull(todoChecklistId);
    }
}
