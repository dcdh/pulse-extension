package com.damdamdeo.pulse.extension.core.query.listener;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.TodoMarkedAsDone;
import com.damdamdeo.pulse.extension.core.listener.AbstractOnStoredEventListener;
import com.damdamdeo.pulse.extension.core.listener.OnStoredEventListenerException;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.query.TodoRepository;

import java.util.Objects;

public final class TodoMarkedAsDoneEventListener extends AbstractOnStoredEventListener<TodoId, TodoMarkedAsDone> {

    private final TodoRepository todoRepository;

    public TodoMarkedAsDoneEventListener(final TodoRepository todoRepository) {
        this.todoRepository = Objects.requireNonNull(todoRepository);
    }

    @Override
    public void on(final TodoId id, final AggregateVersion aggregateVersion, final TodoMarkedAsDone event) throws OnStoredEventListenerException {
        Objects.requireNonNull(id);
        Objects.requireNonNull(aggregateVersion);
        Objects.requireNonNull(event);
        final TodoProjection byTodoId = todoRepository.getByTodoId(id);
        todoRepository.store(byTodoId.withStatus(Status.DONE));
    }

    @Override
    protected Class<TodoMarkedAsDone> eventType() {
        return TodoMarkedAsDone.class;
    }
}
