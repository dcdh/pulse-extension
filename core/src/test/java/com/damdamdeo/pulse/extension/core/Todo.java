package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public final class Todo implements AggregateRoot<TodoId> {

    private TodoId id;
    private String description;
    private Status status;

    public void handle(final CreateTodo createTodo, final EventAppender<TodoId> eventAppender) {
        Objects.requireNonNull(createTodo);
        Objects.requireNonNull(eventAppender);
        eventAppender.append(new NewTodoCreated(createTodo.id(), createTodo.description()));
    }

    public void handle(final MarkTodoAsDone markTodoAsDone, final EventAppender<TodoId> eventAppender) {
        Objects.requireNonNull(markTodoAsDone);
        Objects.requireNonNull(eventAppender);
        eventAppender.append(new TodoMarkedAsDone(markTodoAsDone.id()));
    }

    public void on(final NewTodoCreated newTodoCreated) {
        Objects.requireNonNull(newTodoCreated);
        this.id = newTodoCreated.id();
        this.description = newTodoCreated.description();
        this.status = Status.IN_PROGRESS;
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
