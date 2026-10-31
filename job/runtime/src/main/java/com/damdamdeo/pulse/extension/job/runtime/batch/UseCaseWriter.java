package com.damdamdeo.pulse.extension.job.runtime.batch;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.job.ExecutionContextOverloader;
import com.damdamdeo.pulse.extension.core.job.JobName;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.batch.api.chunk.ItemWriter;
import jakarta.batch.runtime.context.JobContext;

import java.util.List;
import java.util.Objects;

public abstract class UseCaseWriter<A extends AggregateId, C extends Command<A>, AR extends AggregateRoot<A>>
        extends AbstractItemWriter implements ItemWriter {

    private final DomainUseCase<A, C, AR> domainUseCase;
    private final JobContext jobContext;
    private final ExecutionContextOverloader executionContextOverloader;

    protected UseCaseWriter(final DomainUseCase<A, C, AR> domainUseCase,
                            final JobContext jobContext,
                            final ExecutionContextOverloader executionContextOverloader) {
        this.domainUseCase = Objects.requireNonNull(domainUseCase);
        this.jobContext = Objects.requireNonNull(jobContext);
        this.executionContextOverloader = Objects.requireNonNull(executionContextOverloader);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeItems(final List<Object> commands) throws Exception {
        Objects.requireNonNull(commands);
        final JobName jobName = new JobName(jobContext.getJobName());
        executionContextOverloader.overload(jobName);
        for (final Object command : commands) {
            domainUseCase.execute((C) command);
        }
        executionContextOverloader.clean(jobName);
    }
}
