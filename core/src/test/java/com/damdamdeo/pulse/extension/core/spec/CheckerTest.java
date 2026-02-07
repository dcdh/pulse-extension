package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThatCode(() -> IMPORTANT_NEXT_IN_PROGRESS.check(givenTodo)).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenTodoIsUnknown() {
        // Given

        // When && Then
        assertThatThrownBy(() -> IMPORTANT_NEXT_IN_PROGRESS.check((Todo) null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("la todo est inconnu");
    }

    @Test
    void shouldFailWhenTodoIsUnknownUsingSuppler() {
        // Given

        // When && Then
        assertThatThrownBy(() -> IMPORTANT_NEXT_IN_PROGRESS.check(() -> null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("la todo est inconnu");
    }

    @Test
    void shouldFailWhenTodoIsNotImportant() {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", Status.IN_PROGRESS, false);

        // When && Then
        assertThatThrownBy(() -> IMPORTANT_NEXT_IN_PROGRESS.check(givenTodo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("la todo Damien/0 doit être importante");
    }

    @Test
    void shouldFailWhenTodoIsImportantAndNotInProgress() {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", Status.DONE, true);

        // When && Then
        assertThatThrownBy(() -> IMPORTANT_NEXT_IN_PROGRESS.check(givenTodo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("la todo Damien/0 doit être in progress");
    }

}
