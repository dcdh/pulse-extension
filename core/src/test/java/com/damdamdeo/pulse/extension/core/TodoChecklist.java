package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.command.AddNewTodoItem;
import com.damdamdeo.pulse.extension.core.event.EventAppender;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.TodoItemAdded;

import java.util.Objects;

public final class TodoChecklist extends AggregateRoot<TodoChecklistId> {

    private String description;

    public TodoChecklist(final TodoChecklistId todoChecklistId) {
        super(todoChecklistId);
    }

    public TodoChecklist(final TodoChecklistId todoChecklistId,
                         final String description) {
        super(todoChecklistId);
        this.description = Objects.requireNonNull(description);
    }

    public void handle(final AddNewTodoItem addNewTodoItem, final EventAppender eventAppender) {
        Objects.requireNonNull(addNewTodoItem);
        Objects.requireNonNull(eventAppender);
        eventAppender.append(new TodoItemAdded(addNewTodoItem.description()));
    }

    public void on(final TodoItemAdded todoItemAdded) {
        Objects.requireNonNull(todoItemAdded);
        this.description = todoItemAdded.description();
    }

    public String description() {
        return description;
    }

    @Override
    public TodoChecklistId id() {
        return id;
    }

    @Override
    public BelongsTo belongsTo() {
        return new BelongsTo(id.todoId());
    }

    @Override
    public OwnedBy ownedBy() {
        return new OwnedBy(id.todoId().user());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TodoChecklist that = (TodoChecklist) o;
        return Objects.equals(id, that.id)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, description);
    }

    @Override
    public String toString() {
        return "TodoChecklist{" +
                ", description='" + description + '\'' +
                ", id=" + id +
                '}';
    }
}
