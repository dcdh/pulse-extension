package com.damdamdeo.pulse.extension.it.infra.query;

import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.it.domain.ListTodos;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class FailingProjectionQuery implements Query<ListTodos, TodoProjection> {

    @Override
    public Result<TodoProjection> execute(final ListTodos input) throws QueryException {
        throw new UnsupportedOperationException("Should not be called because of the guard query");
    }

    // By returning an empty list, we are telling the guard query that this query is not relevant for the current user
    @Override
    public List<Audience> audiences() {
        return List.of();
    }
}
