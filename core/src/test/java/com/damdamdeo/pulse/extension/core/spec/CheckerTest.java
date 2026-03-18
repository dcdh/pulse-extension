package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.executedby.NotAvailableExecutionContextProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class CheckerTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new NotAvailableExecutionContextProvider().provide();

    private static final Checker<Todo> IMPORTANT_NEXT_IN_PROGRESS = Checker.<Todo>builder()
            .step(Step.<Todo>builder()
                    .spec(new TodoIsImportantSpec())
                    .exceptionProvider((todo) -> new IllegalStateException("la todo %s doit être importante".formatted(todo.id().id())))
                    .build())
            .step(Step.<Todo>builder()
                    .spec(new TodoIsInProgressSpec())
                    .exceptionProvider((todo) -> new IllegalStateException("la todo %s doit être in progress".formatted(todo.id().id())))
                    .build())
            .build();

    @Test
    void shouldNotFailWhenTodoIsImportantAndInProgress() {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", Status.IN_PROGRESS, true);

        // When && Then
        assertAll(
                () -> assertThatCode(() -> IMPORTANT_NEXT_IN_PROGRESS.check(givenTodo, EXECUTION_CONTEXT)).doesNotThrowAnyException(),
                () -> assertThat(IMPORTANT_NEXT_IN_PROGRESS.isSatisfiedBy(givenTodo, EXECUTION_CONTEXT)).isTrue());
    }

    @Test
    void shouldFailWhenTodoIsNotImportant() {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", Status.IN_PROGRESS, false);

        // When && Then
        assertAll(
                () -> assertThatThrownBy(() -> IMPORTANT_NEXT_IN_PROGRESS.check(givenTodo, EXECUTION_CONTEXT))
                        .isInstanceOf(BusinessException.class)
                        .hasRootCauseInstanceOf(IllegalStateException.class)
                        .hasRootCauseMessage("la todo Damien/0 doit être importante"),
                () -> assertThat(IMPORTANT_NEXT_IN_PROGRESS.isSatisfiedBy(givenTodo, EXECUTION_CONTEXT)).isFalse());
    }

    @Test
    void shouldFailWhenTodoIsImportantAndNotInProgress() {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", Status.DONE, true);

        // When && Then
        assertAll(
                () -> assertThatThrownBy(() -> IMPORTANT_NEXT_IN_PROGRESS.check(givenTodo, EXECUTION_CONTEXT))
                        .isInstanceOf(BusinessException.class)
                        .hasRootCauseInstanceOf(IllegalStateException.class)
                        .hasRootCauseMessage("la todo Damien/0 doit être in progress"),
                () -> assertThat(IMPORTANT_NEXT_IN_PROGRESS.isSatisfiedBy(givenTodo, EXECUTION_CONTEXT)).isFalse());
    }
}
