package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.executedby.NotAvailableExecutionContextProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NullableInputSpecificationTest {

    private static ExecutionContext EXECUTION_CONTEXT = new NotAvailableExecutionContextProvider().provide();

    private static final Specification<Todo> NULLABLE = new NullableInputSpecification<>();

    @Test
    void shouldReturnFalseWhenInputIsNull() {
        // Given

        // When
        final boolean satisfiedBy = NULLABLE.isSatisfiedBy(null, EXECUTION_CONTEXT);

        // Then
        assertFalse(satisfiedBy);
    }

    @Test
    void shouldReturnTrueWhenInputIsNotNull() {
        // Given
        final Todo givenTodo = new Todo(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), "lorem", Status.IN_PROGRESS, false);

        // When
        final boolean satisfiedBy = NULLABLE.isSatisfiedBy(givenTodo, EXECUTION_CONTEXT);

        // Then
        assertTrue(satisfiedBy);
    }
}
