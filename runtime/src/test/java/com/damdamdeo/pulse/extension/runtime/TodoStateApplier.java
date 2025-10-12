package com.damdamdeo.pulse.extension.runtime;

import java.util.List;

public class TodoStateApplier extends StateApplier<Todo, TodoId> {

    public TodoStateApplier(final List<Event<TodoId>> events) {
        super(new ReflectionAggregateRootInstanceCreator(), events);
    }

    @Override
    protected Class<Todo> getAggregateClass() {
        return Todo.class;
    }
}
