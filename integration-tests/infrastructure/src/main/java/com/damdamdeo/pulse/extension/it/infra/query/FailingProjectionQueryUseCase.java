package com.damdamdeo.pulse.extension.it.infra.query;

import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.QueryUseCase;
import com.damdamdeo.pulse.extension.core.query.Result;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;
import com.damdamdeo.pulse.extension.it.domain.ListTodos;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class FailingProjectionQueryUseCase implements QueryUseCase<ListTodos, TodoProjection> {

    @Override
    public Result<TodoProjection> execute(final ListTodos input) throws QueryException {
        return Result.of(List.of(), Set.of());
    }

    // By returning an empty list, we are telling the guard query that this query is not relevant for the current user
    @Override
    public List<Permission> permissions() {
        return List.of();
    }
}
