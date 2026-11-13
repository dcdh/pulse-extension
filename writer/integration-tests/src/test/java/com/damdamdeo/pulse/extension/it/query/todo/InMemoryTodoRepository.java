package com.damdamdeo.pulse.extension.it.query.todo;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.query.TodoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Supports the projection listeners discovered in the pulse-core test fixtures.
@ApplicationScoped
public class InMemoryTodoRepository implements TodoRepository {

    private final Map<TodoId, TodoProjection> todos = new ConcurrentHashMap<>();

    @Override
    public void store(final TodoProjection projection) {
        todos.put(projection.id().linkTo(), projection);
    }

    @Override
    public TodoProjection getByTodoId(final TodoId todoId) {
        return todos.get(todoId);
    }

    @Override
    public Collection<TodoProjection> findAll() {
        return List.copyOf(todos.values());
    }
}
