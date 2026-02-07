package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotSpecificationTest {

    private static final Specification<Todo> NOT_IMPORTANT = new TodoIsImportantSpec().not();

    @Test
    void shouldBeTrueWhenTodoIsNotImportant() {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", Status.IN_PROGRESS, false);

        // When
        final boolean isSatisfiedBy = NOT_IMPORTANT.isSatisfiedBy(givenTodo);

        // Then
        assertThat(isSatisfiedBy).isTrue();
    }

    @Test
    void shouldBeFalseWhenTodoIsImportant() {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", Status.IN_PROGRESS, true);

        // When
        final boolean isSatisfiedBy = NOT_IMPORTANT.isSatisfiedBy(givenTodo);

        // Then
        assertThat(isSatisfiedBy).isFalse();
    }
}
