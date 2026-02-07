package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NullableInputSpecificationTest {

    private static final Specification<Todo> NULLABLE = new NullableInputSpecification<>();

    @Test
    void shouldReturnFalseWhenInputIsNull() {
        // Given

        // When
        final boolean satisfiedBy = NULLABLE.isSatisfiedBy(null);

        // Then
        assertFalse(satisfiedBy);
    }

    @Test
    void shouldReturnTrueWhenInputIsNotNull() {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", Status.IN_PROGRESS, false);

        // When
        final boolean satisfiedBy = NULLABLE.isSatisfiedBy(givenTodo);

        // Then
        assertTrue(satisfiedBy);
    }
}
