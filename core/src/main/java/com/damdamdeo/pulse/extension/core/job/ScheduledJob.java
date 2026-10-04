package com.damdamdeo.pulse.extension.core.job;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public abstract class ScheduledJob<A extends AggregateId, I extends Input, P extends Projection<A>, R extends MultiplePageableResult<A, P>,
        C extends Command<A>, AR extends AggregateRoot<A>> {

    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private final JobUnblockingLockManager jobUnblockingLockManager;
    private final ExecutionContextOverloader executionContextOverloader;
    private final QueryUseCase<A, I, P, R> queryUseCase;
    private final DomainUseCase<A, C, AR> domainUseCase;

    protected ScheduledJob(final JobUnblockingLockManager jobUnblockingLockManager,
                           final ExecutionContextOverloader executionContextOverloader,
                           final QueryUseCase<A, I, P, R> queryUseCase,
                           final DomainUseCase<A, C, AR> domainUseCase) {
        this.jobUnblockingLockManager = Objects.requireNonNull(jobUnblockingLockManager);
        this.executionContextOverloader = Objects.requireNonNull(executionContextOverloader);
        this.queryUseCase = Objects.requireNonNull(queryUseCase);
        this.domainUseCase = Objects.requireNonNull(domainUseCase);
    }

    public final void execute() throws JobExecutionException {
        final JobName jobName = jobName();
        try {
            jobUnblockingLockManager.executeWithUnblockingLock(jobName,
                    () -> {
                        try {
                            LOGGER.info("Starting job %s".formatted(jobName));
                            executionContextOverloader.overload(jobName);
                            boolean hasMore;
                            int currentPage = 0;
                            do {
                                final Pagination pagination = new Pagination(currentPage, chunkSize().size());
                                final R executed = queryUseCase.execute(input(), pagination);
                                final List<C> commandsToExecute = process(executed);
                                for (final C command : commandsToExecute) {
                                    try {
                                        domainUseCase.execute(command);
                                    } catch (final UseCaseException exception) {
                                        if (exception.isBusinessFailure()) {
                                            LOGGER.warning("Job %s fail to process domain use case with business failure - continue to process other commands".formatted(jobName));
                                        }
                                        throw new JobExecutionException(exception);
                                    }
                                }
                                currentPage++;
                                hasMore = executed.hasNext();
                            } while (hasMore);
                            LOGGER.info("Job %s finished".formatted(jobName));
                        } catch (final QueryException exception) {
                            throw new JobExecutionException(exception);
                        } finally {
                            executionContextOverloader.clean(jobName);
                        }
                    });
        } catch (final JobLockingException exception) {
            throw new JobExecutionException(exception);
        }
    }

    protected abstract I input();

    protected abstract List<C> process(R result);

    protected abstract ChunkSize chunkSize();

    public abstract Cron cron();

    public JobName jobName() {
        return new JobName(this.getClass().getSimpleName());
    }
}
