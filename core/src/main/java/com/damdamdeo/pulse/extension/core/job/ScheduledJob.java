package com.damdamdeo.pulse.extension.core.job;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

import java.util.*;
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
                            final Set<A> firstPageExecution = new HashSet<>();
                            final Set<A> secondPageExecution = new HashSet<>();
                            executionContextOverloader.overload(jobName);
                            IncrementStrategy incrementStrategy = IncrementStrategy.UNKNOWN;
                            boolean hasMore = true;
                            int currentPage = 0;
                            do {
                                final Pagination pagination = new Pagination(currentPage, chunkSize().size());
                                final R executed = queryUseCase.execute(input(), pagination);
                                if (!pagination.loadAll()) {
                                    if (currentPage == 0 && firstPageExecution.isEmpty()) {
                                        firstPageExecution.addAll(executed.aggregateIds());
                                    } else if (currentPage == 0) {
                                        secondPageExecution.addAll(executed.aggregateIds());
                                        if (Collections.disjoint(firstPageExecution, secondPageExecution)) {
                                            // two runs at page 0 do not return the same dataset, so pagination needs to be kept at 0 to process all the dataset until reaching the end
                                            incrementStrategy = IncrementStrategy.KEEP_ZERO_PAGINATION;
                                        } else {
                                            // two runs at page 0 return the same dataset by aggregate identifier, so pagination needs to be incremented to avoid processing the same dataset twice
                                            incrementStrategy = IncrementStrategy.INCREMENT_PAGINATION;
                                            continue;// avoid processing the dataset twice
                                        }
                                    }
                                }
                                currentPage = switch (incrementStrategy) {
                                    case UNKNOWN, KEEP_ZERO_PAGINATION -> 0;
                                    case INCREMENT_PAGINATION -> currentPage + 1;
                                };
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

    enum IncrementStrategy {
        UNKNOWN,
        INCREMENT_PAGINATION,
        KEEP_ZERO_PAGINATION;
    }

    protected abstract I input();

    protected abstract List<C> process(R result);

    protected abstract ChunkSize chunkSize();

    public abstract Cron cron();

    public JobName jobName() {
        return new JobName(this.getClass().getSimpleName());
    }
}
