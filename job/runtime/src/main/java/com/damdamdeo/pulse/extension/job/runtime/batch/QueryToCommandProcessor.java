package com.damdamdeo.pulse.extension.job.runtime.batch;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.query.Projection;
import jakarta.batch.api.chunk.ItemProcessor;

import java.util.Objects;
import java.util.logging.Logger;

public abstract class QueryToCommandProcessor<A extends AggregateId, C extends Command<A>, P extends Projection<A>>
        implements ItemProcessor {

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    @Override
    public final Object processItem(final Object item) throws Exception {
        Objects.requireNonNull(item);
        LOGGER.fine("Processing item: " + item);
        return toCommand(getProjectionClass().cast(item));
    }

    protected abstract C toCommand(P projection);

    protected abstract Class<P> getProjectionClass();
}
