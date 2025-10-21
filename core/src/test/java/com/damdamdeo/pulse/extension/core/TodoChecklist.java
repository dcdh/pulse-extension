package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.command.AddNewTodoItem;
import com.damdamdeo.pulse.extension.core.event.EventAppender;
import com.damdamdeo.pulse.extension.core.event.TodoItemAdded;

import java.util.Objects;

public final class TodoChecklist implements AggregateRoot<TodoChecklistId> {

    private TodoChecklistId todoChecklistId;
    private String description;

    public void handle(final AddNewTodoItem addNewTodoItem, final EventAppender<TodoChecklistId> eventAppender) {
        Objects.requireNonNull(addNewTodoItem);
        Objects.requireNonNull(eventAppender);
        eventAppender.append(new TodoItemAdded(
                addNewTodoItem.id(),
                addNewTodoItem.description()));
    }

    public void on(final TodoItemAdded todoItemAdded) {
        Objects.requireNonNull(todoItemAdded);
        this.todoChecklistId = todoItemAdded.id();
        this.description = todoItemAdded.description();
    }

    public String description() {
        return description;
    }

    @Override
    public TodoChecklistId id() {
        return todoChecklistId;
    }

    @Override
    public InRelationWith inRelationWith() {
        return new InRelationWith(todoChecklistId.todoId());
    }
}
