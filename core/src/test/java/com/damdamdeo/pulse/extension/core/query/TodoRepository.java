package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.TodoId;

import java.util.Collection;

public interface TodoRepository {

    void store(TodoProjection todoProjection);

    TodoProjection getByTodoId(TodoId todoId);

    Collection<TodoProjection> findAll();
}
