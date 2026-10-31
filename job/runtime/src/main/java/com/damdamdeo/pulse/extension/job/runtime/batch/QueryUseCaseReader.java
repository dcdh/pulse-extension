package com.damdamdeo.pulse.extension.job.runtime.batch;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.job.SimpleScheduledJob;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.query.Input;
import com.damdamdeo.pulse.extension.core.query.MultiplePageableResult;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.QueryUseCase;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.api.chunk.ItemReader;
import jakarta.batch.runtime.context.JobContext;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Properties;
import java.util.Queue;

/**
 * Only pagination all is supported.
 * TODO use <chunk item-count ...
 *
 * @param <A>
 * @param <I>
 * @param <P>
 * @param <R>
 */
public abstract class QueryUseCaseReader<A extends AggregateId, I extends Input, P extends Projection<A>,
        R extends MultiplePageableResult<A, P>> extends AbstractItemReader implements ItemReader {

    private Queue<P> toProcess;
    private I input;

    private final QueryUseCase<A, I, P, R> queryUseCase;
    private final JobContext jobContext;

    protected QueryUseCaseReader(final QueryUseCase<A, I, P, R> queryUseCase, final JobContext jobContext) {
        this.queryUseCase = Objects.requireNonNull(queryUseCase);
        this.jobContext = Objects.requireNonNull(jobContext);
    }

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        input = input(jobContext.getProperties());
        final Pagination pagination = Pagination.all();
        final R executed = queryUseCase.execute(input, pagination);
        toProcess = new ArrayDeque<>(executed.projections());
    }

    /**
     * @return
     * @throws Exception
     * @see SimpleScheduledJob#execute
     */
    @Override
    public Object readItem() throws Exception {
        if (toProcess.isEmpty()) {
            return null;
        }
        return toProcess.poll();
    }

    protected abstract I input(Properties parameters);
}
