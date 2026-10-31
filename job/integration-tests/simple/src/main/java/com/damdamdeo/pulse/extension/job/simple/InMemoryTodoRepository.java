package com.damdamdeo.pulse.extension.job.simple;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.query.TodoRepository;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class InMemoryTodoRepository implements TodoRepository {

    final Map<TodoId, TodoProjection> todos = new HashMap<>();

    @Override
    public void store(final TodoProjection todoProjection) {
        Objects.requireNonNull(todoProjection);
        this.todos.put(todoProjection.id().linkTo(), todoProjection);
    }

    @Override
    public TodoProjection getByTodoId(final TodoId todoId) {
        return todos.get(todoId);
    }

    @Override
    public Collection<TodoProjection> findAll() {
        return todos.values();
    }
}
