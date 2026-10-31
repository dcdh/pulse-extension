package com.damdamdeo.pulse.extension.core.query.listener;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.TodoArchived;
import com.damdamdeo.pulse.extension.core.listener.AbstractOnStoredEventListener;
import com.damdamdeo.pulse.extension.core.listener.OnStoredEventListenerException;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.query.TodoRepository;

import java.util.Objects;

public class TodoArchivedEventListener extends AbstractOnStoredEventListener<TodoId, TodoArchived> {

    private final TodoRepository todoRepository;

    public TodoArchivedEventListener(final TodoRepository todoRepository) {
        this.todoRepository = Objects.requireNonNull(todoRepository);
    }

    @Override
    public void on(final TodoId id, final AggregateVersion aggregateVersion, final TodoArchived event) throws OnStoredEventListenerException {
        Objects.requireNonNull(id);
        Objects.requireNonNull(aggregateVersion);
        Objects.requireNonNull(event);
        final TodoProjection byTodoId = todoRepository.getByTodoId(id);
        todoRepository.store(byTodoId.withStatus(Status.ARCHIVED));
    }

    @Override
    protected Class<TodoArchived> eventType() {
        return TodoArchived.class;
    }
}
