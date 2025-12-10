package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.command.TodoItem;

import java.util.List;
import java.util.Objects;

public record MultipleTodoItemsAdded(List<TodoItem> todoItems) implements Event {

    public MultipleTodoItemsAdded {
        Objects.requireNonNull(todoItems);
    }
}
