package com.damdamdeo.pulse.extension.job.jberet;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.query.ListDoneTodos;
import com.damdamdeo.pulse.extension.core.query.MultiplePageableResult;
import com.damdamdeo.pulse.extension.core.query.PaginatedResultQueryUseCase;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.job.runtime.batch.QueryUseCaseReader;
import jakarta.batch.runtime.context.JobContext;

import java.util.Objects;
import java.util.Properties;

public class ListDoneTodosQueryUseCaseReader extends QueryUseCaseReader<TodoId, ListDoneTodos, TodoProjection,
        MultiplePageableResult<TodoId, TodoProjection>> {

    protected ListDoneTodosQueryUseCaseReader(final PaginatedResultQueryUseCase<TodoId, ListDoneTodos, TodoProjection,
                                                      MultiplePageableResult<TodoId, TodoProjection>> queryUseCase,
                                              final JobContext jobContext) {
        super(queryUseCase, jobContext);
    }

    @Override
    protected ListDoneTodos input(final Properties parameters) {
        Objects.requireNonNull(parameters);
        return new ListDoneTodos();
    }
}
