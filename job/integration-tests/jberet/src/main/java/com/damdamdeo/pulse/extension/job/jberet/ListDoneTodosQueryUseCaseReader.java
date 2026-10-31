package com.damdamdeo.pulse.extension.job.jberet;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.query.ListDoneTodos;
import com.damdamdeo.pulse.extension.core.query.MultiplePageableResult;
import com.damdamdeo.pulse.extension.core.query.QueryUseCase;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.job.runtime.batch.QueryUseCaseReader;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

import java.util.Objects;
import java.util.Properties;

@Dependent
@Named
public class ListDoneTodosQueryUseCaseReader extends QueryUseCaseReader<TodoId, ListDoneTodos, TodoProjection,
        MultiplePageableResult<TodoId, TodoProjection>> {

    protected ListDoneTodosQueryUseCaseReader(final QueryUseCase<TodoId, ListDoneTodos, TodoProjection,
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
