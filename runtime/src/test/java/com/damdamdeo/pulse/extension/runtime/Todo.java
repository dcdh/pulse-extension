package com.damdamdeo.pulse.extension.runtime;

import java.util.Objects;

public final class Todo implements AggregateRoot<TodoId> {

    private TodoId id;
    private String description;
    private Status status;

    public void on(final NewTodoCreated newTodoCreated) {
        Objects.requireNonNull(newTodoCreated);
        this.id = newTodoCreated.id();
        this.description = newTodoCreated.description();
        this.status = Status.DONE;
    }

    public void on(final TodoMarkedAsDone todoMarkedAsDone) {
        Objects.requireNonNull(todoMarkedAsDone);
        this.status = Status.DONE;
    }

    @Override
    public TodoId id() {
        return id;
    }

    public String description() {
        return description;
    }

    public Status status() {
        return status;
    }
}
