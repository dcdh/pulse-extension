package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.Todo;

public class TodoIsImportantSpec extends CompositeSpecification<Todo> {

    @Override
    public boolean isSatisfiedBy(final Todo todo) {
        return todo.important();
    }
}
