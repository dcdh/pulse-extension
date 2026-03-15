package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.command.CommandWithoutOnEvent;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.command.FailTodo;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

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

    public void handle(final CreateTodo createTodo, final ExecutionContext executionContext, final EventAppender eventAppender) throws BusinessException {
        Objects.requireNonNull(createTodo);
        Objects.requireNonNull(eventAppender);
        Objects.requireNonNull(executionContext);
        eventAppender.append(new NewTodoCreated(createTodo.description()));
        if (description.startsWith(IMPORTANT)) {
            eventAppender.append(new ClassifiedAsImportant());
        }
    }

    public void handle(final MarkTodoAsDone markTodoAsDone, final ExecutionContext executionContext, final EventAppender eventAppender) throws BusinessException {
        Objects.requireNonNull(markTodoAsDone);
        Objects.requireNonNull(eventAppender);
        Objects.requireNonNull(executionContext);
        eventAppender.append(new TodoMarkedAsDone());
    }

    public void handle(final FailTodo failTodo, final ExecutionContext executionContext, final EventAppender eventAppender) throws BusinessException {
        Objects.requireNonNull(failTodo);
        Objects.requireNonNull(eventAppender);
        Objects.requireNonNull(executionContext);
        throw new BusinessException(new IllegalStateException("Fail !"));
    }

    public void handle(final CommandWithoutOnEvent commandWithoutOnEvent, final ExecutionContext executionContext, final EventAppender eventAppender) throws BusinessException {
        Objects.requireNonNull(commandWithoutOnEvent);
        Objects.requireNonNull(eventAppender);
        Objects.requireNonNull(executionContext);
        eventAppender.append(new Missing());
    }

    public void on(final NewTodoCreated newTodoCreated, final ExecutedBy executedBy) {
        Objects.requireNonNull(newTodoCreated);
        Objects.requireNonNull(executedBy);
        this.description = newTodoCreated.description();
        this.status = Status.IN_PROGRESS;
    }

    public void on(final TodoMarkedAsDone todoMarkedAsDone, final ExecutedBy executedBy) {
        Objects.requireNonNull(todoMarkedAsDone);
        Objects.requireNonNull(executedBy);
        this.status = Status.DONE;
    }

    public void on(final ClassifiedAsImportant classifiedAsImportant, final ExecutedBy executedBy) {
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

    public boolean isInProgress() {
        return Status.IN_PROGRESS.equals(status);
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
