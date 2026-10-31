package com.damdamdeo.pulse.extension.job.jberet;

import com.damdamdeo.pulse.extension.core.command.ArchiveTodo;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import io.quarkus.logging.Log;
import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

import java.util.Objects;

@Dependent
@Named
public class TodoItemProcessor implements ItemProcessor {

    @Override
    public Object processItem(final Object item) {
        Objects.requireNonNull(item);
        Log.info("Processing item: " + item);
        return new ArchiveTodo(((TodoProjection) item).id().linkTo());
    }
}
