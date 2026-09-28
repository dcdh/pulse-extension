package com.damdamdeo.pulse.extension.it.infra.query;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.QueryUseCase;
import com.damdamdeo.pulse.extension.core.query.Result;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;
import com.damdamdeo.pulse.extension.it.domain.ListTodos;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class FailingProjectionQueryUseCase implements QueryUseCase<TodoId, ListTodos, TodoProjection> {

    @Override
    public Result<TodoId, TodoProjection> execute(final ListTodos input) throws QueryException {
        return Result.of(List.of());
    }

    // By returning an empty list, we are telling the guard query that this query is not relevant for the current user
    @Override
    public List<Permission<TodoId>> permissions() {
        return List.of();
    }
}
