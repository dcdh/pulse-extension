package com.damdamdeo.pulse.extension.job.jberet;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.command.Handled;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.usecase.CreateTodoDomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.MarkTodoAsDoneUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

@QuarkusTest
class JberetJobTest {

    @Inject
    CreateTodoDomainUseCase createTodoDomainUseCase;

    @Inject
    MarkTodoAsDoneUseCase markTodoAsDoneUseCase;

    @Inject
    JobOperator jobOperator;

    @Inject
    InMemoryTodoRepository inMemoryTodoRepository;

    @Test
    void shouldJobBeRegistered() {
        assertThat(jobOperator.getJobNames()).contains("archiveTodosJob");
    }

    @Test
    @RunOnVertxContext(runOnEventLoop = false)
    void shouldExecuteJberetJob() throws UseCaseException {
        // Given
        final Handled<Todo, TodoId> loremIpsum = createTodoDomainUseCase.execute(new CreateTodo("lorem ipsum", new Owner(UserId.USER_1)));
        markTodoAsDoneUseCase.execute(new MarkTodoAsDone(loremIpsum.id()));

        // When
        final long executionId = jobOperator.start("archiveTodosJob", new Properties());

        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> BatchStatus.COMPLETED.equals(jobOperator
                .getJobExecution(executionId).getBatchStatus()));

        assertAll(
                () -> assertThat(inMemoryTodoRepository.findAll().size()).isEqualTo(1),
                () -> assertThat(inMemoryTodoRepository.findAll().iterator().next().status()).isEqualTo(Status.ARCHIVED)
        );
    }
}
