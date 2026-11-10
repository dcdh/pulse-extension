package com.damdamdeo.pulse.extension.job.jberet;

import com.damdamdeo.pulse.extension.core.TodoAggregateRoot;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.ArchiveTodo;
import com.damdamdeo.pulse.extension.core.job.ExecutionContextOverloader;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.job.runtime.batch.UseCaseWriter;
import jakarta.batch.runtime.context.JobContext;

public class ArchiveTodoDomainUseCaseWriter extends UseCaseWriter<TodoId, ArchiveTodo, TodoAggregateRoot> {

    protected ArchiveTodoDomainUseCaseWriter(final DomainUseCase<TodoId, ArchiveTodo, TodoAggregateRoot> domainUseCase,
                                             final JobContext jobContext,
                                             final ExecutionContextOverloader executionContextOverloader) {
        super(domainUseCase, jobContext, executionContextOverloader);
    }
}
