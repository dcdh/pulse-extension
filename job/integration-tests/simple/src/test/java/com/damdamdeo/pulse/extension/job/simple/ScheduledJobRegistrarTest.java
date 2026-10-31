package com.damdamdeo.pulse.extension.job.simple;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.command.Handled;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.usecase.CreateTodoDomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.MarkTodoAsDoneUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScheduledJobRegistrarTest {

    @Inject
    CreateTodoDomainUseCase createTodoDomainUseCase;

    @Inject
    MarkTodoAsDoneUseCase markTodoAsDoneUseCase;

    @Inject
    InMemoryTodoRepository inMemoryTodoRepository;

    @Inject
    Scheduler scheduler;

    @BeforeAll
    void setUp() {
        scheduler.pause(ArchiveTodoSimpleScheduledJob.JOB_NAME);
    }

    @Test
    @RunOnVertxContext(runOnEventLoop = false)
    void shouldExecuteScheduledJob() throws UseCaseException {
        // Given
        final Handled<Todo, TodoId> loremIpsum = createTodoDomainUseCase.execute(new CreateTodo("lorem ipsum", new Owner(UserId.USER_1)));
        markTodoAsDoneUseCase.execute(new MarkTodoAsDone(loremIpsum.id()));

        // When
        scheduler.resume(ArchiveTodoSimpleScheduledJob.JOB_NAME);

        // Then
        await().atMost(60, TimeUnit.SECONDS).until(() ->
                inMemoryTodoRepository.findAll().size() == 1
                        && inMemoryTodoRepository.findAll().iterator().next().status().equals(Status.ARCHIVED));
    }
}
