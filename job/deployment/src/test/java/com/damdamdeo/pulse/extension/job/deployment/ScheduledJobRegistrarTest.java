package com.damdamdeo.pulse.extension.job.deployment;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.MissingAggregateException;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.CloseTodo;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.job.*;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.query.permission.ExecutedBySpecificJob;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;
import com.damdamdeo.pulse.extension.core.usecase.AbstractDomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ScheduledJobRegistrarTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideRuntimeConfigKey("quarkus.scheduler.enabled", "true");

    @Inject
    ListTodosQueryUsecase listTodosQueryUsecase;

    @Inject
    ClosedExpiredTodoScheduledJob closedExpiredTodoScheduledJob;

    public record ListTodos() implements Input {
    }

    @ApplicationScoped
    @Unremovable
    public static class ListTodosQueryUsecase implements QueryUseCase<TodoId, ListTodos, TodoProjection, MultiplePageableResult<TodoId, TodoProjection>> {

        private boolean called = false;

        @Override
        public MultiplePageableResult<TodoId, TodoProjection> execute(final ListTodos input, final Pagination pagination) throws QueryException {
            Objects.requireNonNull(input);
            Objects.requireNonNull(pagination);
            called = true;
            return new MultiplePageableResult<>(new Page<>(List.of(), pagination, 0));
        }

        @Override
        public List<Permission<TodoId>> permissions() {
            return List.of(new ExecutedBySpecificJob<>(new JobName("ClosedExpiredTodoScheduledJob")));
        }

        public boolean isCalled() {
            return called;
        }
    }

    public static class CloseDomainUseCase extends AbstractDomainUseCase<TodoId, CloseTodo, Todo> {

        protected CloseDomainUseCase(final CommandHandler<Todo, TodoId> commandHandler) {
            super(commandHandler);
        }

        @Override
        protected Supplier<MissingAggregateException> missingAggregateException() {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public List<com.damdamdeo.pulse.extension.core.usecase.permission.Permission<TodoId, CloseTodo>> permissions() {
            throw new IllegalStateException("Should not be called");
        }
    }

    public static class ClosedExpiredTodoScheduledJob extends ScheduledJob<TodoId, ListTodos, TodoProjection, MultiplePageableResult<TodoId, TodoProjection>,
            CloseTodo, Todo> {

        private final ExecutionContextProvider executionContextProvider;

        private ExecutionContext executionContext;

        protected ClosedExpiredTodoScheduledJob(
                final JobUnblockingLockManager jobUnblockingLockManager,
                final ExecutionContextOverloader executionContextOverloader,
                final QueryUseCase<TodoId, ListTodos, TodoProjection, MultiplePageableResult<TodoId, TodoProjection>> queryUseCase,
                final DomainUseCase<TodoId, CloseTodo, Todo> domainUseCase,
                final ExecutionContextProvider executionContextProvider) {
            super(jobUnblockingLockManager, executionContextOverloader, queryUseCase, domainUseCase);
            this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        }

        @Override
        protected ListTodos input() {
            return new ListTodos();
        }

        @Override
        protected List<CloseTodo> process(final MultiplePageableResult<TodoId, TodoProjection> result) {
            executionContext = executionContextProvider.provide();
            return List.of();
        }

        @Override
        protected ChunkSize chunkSize() {
            return new ChunkSize(10);
        }

        @Override
        public Cron cron() {
            return new Cron("*/5 * * * * ?");
        }

        public ExecutionContext getExecutionContext() {
            return executionContext;
        }
    }

    @Test
    @RunOnVertxContext(runOnEventLoop = false)
    void shouldExecuteScheduledJob() {
        // Given

        // When
        await().atMost(60, TimeUnit.SECONDS).until(() -> listTodosQueryUsecase.isCalled());

        // Then
        final ExecutionContext provided = closedExpiredTodoScheduledJob.getExecutionContext();
        assertThat(provided).isEqualTo(new ExecutionContext(new ExecutedBy.Job(new JobName("ClosedExpiredTodoScheduledJob")), Set.of()));
    }
}
