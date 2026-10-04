package com.damdamdeo.pulse.extension.core.job;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseExceptionCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ScheduledJobTest {

    private static final JobName JOB_NAME = new JobName("TestScheduledJob");
    private static final TestInput INPUT = new TestInput();
    private static final ChunkSize CHUNK_SIZE = new ChunkSize(25);

    @Mock
    private JobUnblockingLockManager jobUnblockingLockManager;

    @Mock
    private ExecutionContextOverloader executionContextOverloader;

    @Mock
    private QueryUseCase<TestId, TestInput, TestProjection, MultiplePageableResult<TestId, TestProjection>> queryUseCase;

    @Mock
    private DomainUseCase<TestId, TestCommand, TestAggregate> domainUseCase;

    @Mock
    private ResultProcessor resultProcessor;

    @Mock
    private MultiplePageableResult<TestId, TestProjection> firstResult;

    @Mock
    private MultiplePageableResult<TestId, TestProjection> secondResult;

    private TestScheduledJob scheduledJob;

    @BeforeEach
    void setUp() {
        scheduledJob = new TestScheduledJob(jobUnblockingLockManager, executionContextOverloader,
                queryUseCase, domainUseCase, resultProcessor
        );
    }

    @Test
    void shouldExecuteEveryCommandAndCleanContextForSinglePage() throws Exception {
        // given
        final TestCommand firstCommand = new TestCommand(new TestId("first"));
        final TestCommand secondCommand = new TestCommand(new TestId("second"));
        executeLockedJobWhenCalled();
        given(queryUseCase.execute(INPUT, new Pagination(0, CHUNK_SIZE.size()))).willReturn(firstResult);
        given(firstResult.aggregateIds()).willReturn(Set.of(firstCommand.id(), secondCommand.id()));
        given(resultProcessor.process(firstResult)).willReturn(List.of(firstCommand, secondCommand));
        given(firstResult.hasNext()).willReturn(false);

        // when
        scheduledJob.execute();

        // then
        then(jobUnblockingLockManager).should()
                .executeWithUnblockingLock(eq(JOB_NAME), any(JobExecutor.class));
        final InOrder inOrder = inOrder(
                executionContextOverloader,
                queryUseCase,
                resultProcessor,
                domainUseCase
        );
        inOrder.verify(executionContextOverloader).overload(JOB_NAME);
        inOrder.verify(queryUseCase).execute(INPUT, new Pagination(0, CHUNK_SIZE.size()));
        inOrder.verify(resultProcessor).process(firstResult);
        inOrder.verify(domainUseCase).execute(firstCommand);
        inOrder.verify(domainUseCase).execute(secondCommand);
        inOrder.verify(executionContextOverloader).clean(JOB_NAME);
    }

    @Test
    void shouldKeepPageAtZeroWhenTwoExecutionsReturnDifferentAggregateIds() throws Exception {
        // given
        final TestCommand firstCommand = new TestCommand(new TestId("first"));
        final TestCommand secondCommand = new TestCommand(new TestId("second"));
        executeLockedJobWhenCalled();
        given(queryUseCase.execute(INPUT, new Pagination(0, CHUNK_SIZE.size())))
                .willReturn(firstResult, secondResult);
        given(firstResult.aggregateIds()).willReturn(Set.of(firstCommand.id()));
        given(secondResult.aggregateIds()).willReturn(Set.of(secondCommand.id()));
        given(resultProcessor.process(firstResult)).willReturn(List.of(firstCommand));
        given(resultProcessor.process(secondResult)).willReturn(List.of(secondCommand));
        given(firstResult.hasNext()).willReturn(true);
        given(secondResult.hasNext()).willReturn(false);

        // when
        scheduledJob.execute();

        // then
        final InOrder inOrder = inOrder(queryUseCase, domainUseCase, executionContextOverloader);
        inOrder.verify(queryUseCase).execute(INPUT, new Pagination(0, CHUNK_SIZE.size()));
        inOrder.verify(domainUseCase).execute(firstCommand);
        inOrder.verify(queryUseCase).execute(INPUT, new Pagination(0, CHUNK_SIZE.size()));
        inOrder.verify(domainUseCase).execute(secondCommand);
        inOrder.verify(executionContextOverloader).clean(JOB_NAME);
    }

    @Test
    void shouldWrapQueryExceptionAndAlwaysCleanContext() throws Exception {
        // given
        final QueryException queryException = new QueryException(
                new RuntimeException("query failure"),
                QueryExceptionCode.INFRASTRUCTURE_FAILURE
        );
        executeLockedJobWhenCalled();
        given(queryUseCase.execute(INPUT, new Pagination(0, CHUNK_SIZE.size()))).willThrow(queryException);

        // when
        final JobExecutionException exception = assertThrows(
                JobExecutionException.class,
                scheduledJob::execute
        );

        // then
        assertSame(queryException, exception.getCause());
        then(executionContextOverloader).should().clean(JOB_NAME);
        then(resultProcessor).shouldHaveNoInteractions();
        then(domainUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldWrapDomainExceptionStopFollowingCommandsAndCleanContext() throws Exception {
        // given
        final TestCommand failingCommand = new TestCommand(new TestId("failing"));
        final TestCommand ignoredCommand = new TestCommand(new TestId("ignored"));
        final UseCaseException useCaseException = new UseCaseException(
                new RuntimeException("domain failure"),
                UseCaseExceptionCode.BUSINESS_FAILURE
        );
        executeLockedJobWhenCalled();
        given(queryUseCase.execute(INPUT, new Pagination(0, CHUNK_SIZE.size())))
                .willReturn(firstResult);
        given(firstResult.aggregateIds()).willReturn(Set.of(failingCommand.id(), ignoredCommand.id()));
        given(resultProcessor.process(firstResult)).willReturn(List.of(failingCommand, ignoredCommand));
        given(domainUseCase.execute(failingCommand)).willThrow(useCaseException);

        // when
        final JobExecutionException exception = assertThrows(
                JobExecutionException.class,
                scheduledJob::execute
        );

        // then
        assertSame(useCaseException, exception.getCause());
        then(domainUseCase).should(never()).execute(ignoredCommand);
        then(executionContextOverloader).should().clean(JOB_NAME);
    }

    @Test
    void shouldWrapLockingExceptionWithoutChangingExecutionContext() throws Exception {
        // given
        final JobLockingException lockingException = new JobLockingException(
                new RuntimeException("lock failure")
        );
        willThrow(lockingException)
                .given(jobUnblockingLockManager)
                .executeWithUnblockingLock(eq(JOB_NAME), any(JobExecutor.class));

        // when
        final JobExecutionException exception = assertThrows(
                JobExecutionException.class,
                scheduledJob::execute
        );

        // then
        assertSame(lockingException, exception.getCause());
        then(executionContextOverloader).shouldHaveNoInteractions();
        then(queryUseCase).shouldHaveNoInteractions();
        then(domainUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldDeriveJobNameFromConcreteClassName() {
        // given
        final String expectedName = "TestScheduledJob";

        // when
        final JobName jobName = scheduledJob.jobName();

        // then
        assertEquals(expectedName, jobName.name());
    }

    private void executeLockedJobWhenCalled() throws Exception {
        willAnswer(invocation -> {
            invocation.getArgument(1, JobExecutor.class).execute();
            return null;
        }).given(jobUnblockingLockManager)
                .executeWithUnblockingLock(eq(JOB_NAME), any(JobExecutor.class));
    }

    public record TestId(String id) implements AggregateId {
    }

    public record TestInput() implements Input {
    }

    public record TestProjection(TestId id) implements Projection<TestId> {
    }

    public record TestCommand(TestId id) implements Command<TestId> {
    }

    public static final class TestAggregate extends AggregateRoot<TestId> {

        private TestAggregate(final TestId id) {
            super(id);
        }

        @Override
        public BelongsTo belongsTo() {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public OwnedBy ownedBy() {
            throw new IllegalStateException("Should not be called");
        }
    }

    public interface ResultProcessor {

        List<TestCommand> process(MultiplePageableResult<TestId, TestProjection> result);
    }

    public static final class TestScheduledJob extends ScheduledJob<
            TestId,
            TestInput,
            TestProjection,
            MultiplePageableResult<TestId, TestProjection>,
            TestCommand,
            TestAggregate> {

        private final ResultProcessor resultProcessor;

        public TestScheduledJob(
                final JobUnblockingLockManager jobUnblockingLockManager,
                final ExecutionContextOverloader executionContextOverloader,
                final QueryUseCase<TestId, TestInput, TestProjection,
                        MultiplePageableResult<TestId, TestProjection>> queryUseCase,
                final DomainUseCase<TestId, TestCommand, TestAggregate> domainUseCase,
                final ResultProcessor resultProcessor) {
            super(jobUnblockingLockManager, executionContextOverloader, queryUseCase, domainUseCase);
            this.resultProcessor = resultProcessor;
        }

        @Override
        protected TestInput input() {
            return INPUT;
        }

        @Override
        protected List<TestCommand> process(
                final MultiplePageableResult<TestId, TestProjection> result) {
            return resultProcessor.process(result);
        }

        @Override
        protected ChunkSize chunkSize() {
            return CHUNK_SIZE;
        }

        @Override
        public Cron cron() {
            return new Cron("0 0 12 * * ?");
        }
    }
}
