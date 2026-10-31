package com.damdamdeo.pulse.extension.core.query.listener;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.listener.AbstractOnStoredEventListener;
import com.damdamdeo.pulse.extension.core.listener.OnStoredEventListenerException;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.query.TodoRepository;

import java.util.List;
import java.util.Objects;

public class NewTodoCreatedEventListener extends AbstractOnStoredEventListener<TodoId, NewTodoCreated> {

    private final TodoRepository todoRepository;

    public NewTodoCreatedEventListener(final TodoRepository todoRepository) {
        this.todoRepository = Objects.requireNonNull(todoRepository);
    }

    @Override
    public void on(final TodoId id, final AggregateVersion aggregateVersion, final NewTodoCreated event) throws OnStoredEventListenerException {
        Objects.requireNonNull(id);
        Objects.requireNonNull(aggregateVersion);
        Objects.requireNonNull(event);
        final TodoProjection todoProjection = new TodoProjection(ProjectionId.from(id), event.description(),
                Status.IN_PROGRESS, false, List.of());
        todoRepository.store(todoProjection);
    }

    @Override
    protected Class<NewTodoCreated> eventType() {
        return NewTodoCreated.class;
    }
}
