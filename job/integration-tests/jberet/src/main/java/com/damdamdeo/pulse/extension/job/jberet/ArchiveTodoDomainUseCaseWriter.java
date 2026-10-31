package com.damdamdeo.pulse.extension.job.jberet;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.ArchiveTodo;
import com.damdamdeo.pulse.extension.core.job.ExecutionContextOverloader;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.job.runtime.batch.UseCaseWriter;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Dependent
@Named
public class ArchiveTodoDomainUseCaseWriter extends UseCaseWriter<TodoId, ArchiveTodo, Todo> {

    protected ArchiveTodoDomainUseCaseWriter(final DomainUseCase<TodoId, ArchiveTodo, Todo> domainUseCase,
                                             final JobContext jobContext,
                                             final ExecutionContextOverloader executionContextOverloader) {
        super(domainUseCase, jobContext, executionContextOverloader);
    }
}
