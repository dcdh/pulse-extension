package com.damdamdeo.pulse.extension.job.simple;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.ArchiveTodo;
import com.damdamdeo.pulse.extension.core.job.*;
import com.damdamdeo.pulse.extension.core.query.ListDoneTodos;
import com.damdamdeo.pulse.extension.core.query.MultiplePageableResult;
import com.damdamdeo.pulse.extension.core.query.QueryUseCase;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ArchiveTodoSimpleScheduledJob extends SimpleScheduledJob<TodoId, ListDoneTodos, TodoProjection,
        MultiplePageableResult<TodoId, TodoProjection>, ArchiveTodo, Todo> {

    public static final String JOB_NAME = ArchiveTodoSimpleScheduledJob.class.getSimpleName();

    private static Logger LOGGER = LoggerFactory.getLogger(ArchiveTodoSimpleScheduledJob.class);

    protected ArchiveTodoSimpleScheduledJob(
            final JobUnblockingLockManager jobUnblockingLockManager,
            final ExecutionContextOverloader executionContextOverloader,
            final QueryUseCase<TodoId, ListDoneTodos, TodoProjection, MultiplePageableResult<TodoId, TodoProjection>> queryUseCase,
            final DomainUseCase<TodoId, ArchiveTodo, Todo> domainUseCase) {
        super(jobUnblockingLockManager, executionContextOverloader, queryUseCase, domainUseCase);
    }

    @Override
    protected ListDoneTodos input() {
        return new ListDoneTodos();
    }

    @Override
    protected ArchiveTodo process(final TodoProjection projection) {
        Objects.requireNonNull(projection);
        LOGGER.info("Processing {}", projection.id());
        return new ArchiveTodo(projection.id().linkTo());
    }

    @Override
    protected ChunkSize chunkSize() {
        return new ChunkSize(10);
    }

    @Override
    public Cron cron() {
        return new Cron("*/5 * * * * ?");
    }

}
