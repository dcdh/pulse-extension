package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.event.*;

import java.util.Objects;

public final class Todo extends AggregateRoot<TodoId> {

    private static final String IMPORTANT = "IMPORTANT";

    private String description;
    private Status status;
    private boolean important = false;

    public Todo(final TodoId id) {
        super(id);
    }

    public Todo(final TodoId id,
                final String description,
                final Status status,
                boolean important) {
        super(id);
        this.description = Objects.requireNonNull(description);
        this.status = Objects.requireNonNull(status);
        this.important = important;
    }

    public void handle(final CreateTodo createTodo, final EventAppender eventAppender) {
        Objects.requireNonNull(createTodo);
        Objects.requireNonNull(eventAppender);
        eventAppender.append(new NewTodoCreated(createTodo.description()));
        if (description.startsWith(IMPORTANT)) {
            eventAppender.append(new ClassifiedAsImportant());
        }
    }

    public void handle(final MarkTodoAsDone markTodoAsDone, final EventAppender eventAppender) {
        Objects.requireNonNull(markTodoAsDone);
        Objects.requireNonNull(eventAppender);
        eventAppender.append(new TodoMarkedAsDone());
    }

    public void on(final NewTodoCreated newTodoCreated) {
        Objects.requireNonNull(newTodoCreated);
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
    public BelongsTo belongsTo() {
        return new BelongsTo(id);
    }

    @Override
    public OwnedBy ownedBy() {
        return new OwnedBy(id.user());
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Todo todo = (Todo) o;
        return important == todo.important
                && Objects.equals(description, todo.description)
                && status == todo.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), description, status, important);
    }

    @Override
    public String toString() {
        return "Todo{" +
                "description='" + description + '\'' +
                ", status=" + status +
                ", important=" + important +
                ", id=" + id +
                '}';
    }
}
