package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoAggregateRoot;

import java.util.Objects;

public final class TodoIsImportantSpec implements CompositeSpecification<TodoAggregateRoot> {

    @Override
    public boolean isSatisfiedBy(final TodoAggregateRoot todo, final ExecutionContext executionContext) {
        Objects.requireNonNull(todo);
        Objects.requireNonNull(executionContext);
        return todo.important();
    }
}
