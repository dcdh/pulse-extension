package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public final class Todo implements AggregateRoot<TodoId> {

    private static final String IMPORTANT = "IMPORTANT";

    private TodoId id;
    private String description;
    private Status status;
    private boolean important = false;

    public void handle(final CreateTodo createTodo, final EventAppender<TodoId> eventAppender) {
        Objects.requireNonNull(createTodo);
        Objects.requireNonNull(eventAppender);
        eventAppender.append(new NewTodoCreated(createTodo.id(), createTodo.description()));
        if (description.startsWith(IMPORTANT)) {
            eventAppender.append(new ClassifiedAsImportant(id));
        }
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

    public void on(final ClassifiedAsImportant classifiedAsImportant) {
        this.important = true;
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

    public boolean important() {
        return important;
    }
}
