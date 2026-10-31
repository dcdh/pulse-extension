package com.damdamdeo.pulse.extension.job.jberet;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.ArchiveTodo;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.job.runtime.batch.QueryToCommandProcessor;

import java.util.Objects;

public class TodoItemProcessor extends QueryToCommandProcessor<TodoId, ArchiveTodo, TodoProjection> {

    @Override
    protected ArchiveTodo toCommand(final TodoProjection projection) {
        Objects.requireNonNull(projection);
        return new ArchiveTodo(projection.id().linkTo());
    }

    @Override
    protected Class<TodoProjection> getProjectionClass() {
        return TodoProjection.class;
    }
}
