package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.Todo;

import java.util.Objects;

public final class TodoIsImportantSpec extends CompositeSpecification<Todo> {

    @Override
    public boolean isSatisfiedBy(final Todo todo, final ExecutionContext executionContext) {
        Objects.requireNonNull(todo);
        Objects.requireNonNull(executionContext);
        return todo.important();
    }
}
