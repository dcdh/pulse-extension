package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class CheckerTest {

    private static final Checker<Todo> IMPORTANT_NEXT_IN_PROGRESS = new Checker<Todo>()
            .next(new NullableInputSpecification<>(), (todo) -> new IllegalStateException("la todo est inconnu"))
            .next(new TodoIsImportantSpec(), (todo) -> new IllegalStateException("la todo %s doit être importante".formatted(todo.id().id())))
            .next(new TodoIsInProgressSpec(), (todo) -> new IllegalStateException("la todo %s doit être in progress".formatted(todo.id().id())));

    @Test
    void shouldNotFailWhenTodoIsImportantAndInProgress() {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", Status.IN_PROGRESS, true);

        // When && Then
        assertAll(
                () -> assertThatCode(() -> IMPORTANT_NEXT_IN_PROGRESS.check(givenTodo)).doesNotThrowAnyException(),
                () -> assertThat(IMPORTANT_NEXT_IN_PROGRESS.isSatisfiedBy(givenTodo)).isTrue());
    }

    @Test
    void shouldFailWhenTodoIsUnknown() {
        // Given

        // When && Then
        assertAll(
                () -> assertThatThrownBy(() -> IMPORTANT_NEXT_IN_PROGRESS.check((Todo) null))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("la todo est inconnu"),
                () -> assertThat(IMPORTANT_NEXT_IN_PROGRESS.isSatisfiedBy((Todo) null)).isFalse());
    }

    @Test
    void shouldFailWhenTodoIsUnknownUsingSuppler() {
        // Given
        final Supplier<Todo> supplier = () -> null;

        // When && Then
        assertAll(
                () -> assertThatThrownBy(() -> IMPORTANT_NEXT_IN_PROGRESS.check(supplier))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("la todo est inconnu"),
                () -> assertThat(IMPORTANT_NEXT_IN_PROGRESS.isSatisfiedBy(supplier)).isFalse());
    }

    @Test
    void shouldFailWhenTodoIsNotImportant() {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", Status.IN_PROGRESS, false);

        // When && Then
        assertAll(
                () -> assertThatThrownBy(() -> IMPORTANT_NEXT_IN_PROGRESS.check(givenTodo))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("la todo Damien/0 doit être importante"),
                () -> assertThat(IMPORTANT_NEXT_IN_PROGRESS.isSatisfiedBy(givenTodo)).isFalse());
    }

    @Test
    void shouldFailWhenTodoIsImportantAndNotInProgress() {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", Status.DONE, true);

        // When && Then
        assertAll(
                () -> assertThatThrownBy(() -> IMPORTANT_NEXT_IN_PROGRESS.check(givenTodo))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("la todo Damien/0 doit être in progress"),
                () -> assertThat(IMPORTANT_NEXT_IN_PROGRESS.isSatisfiedBy(givenTodo)).isFalse());
    }
}
