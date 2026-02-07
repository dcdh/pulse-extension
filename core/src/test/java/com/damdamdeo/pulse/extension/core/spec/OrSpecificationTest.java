package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.executedby.NotAvailableExecutionContextProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrSpecificationTest {

    private static final Specification<Todo> IMPORTANT_OR_IN_PROGRESS = new TodoIsImportantSpec()
            .or(new TodoIsInProgressSpec());

    @ParameterizedTest
    @CsvSource(textBlock = """
            # givenStatus, givenImportant, expected
            IN_PROGRESS, true, true
            IN_PROGRESS, false, true
            DONE, true, true
            DONE, false, false
            """
    )
    void shouldValidateSpecification(final Status givenStatus, final boolean givenImportant, final boolean expected) {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", givenStatus, givenImportant);

        // When
        final boolean isSatisfiedBy = IMPORTANT_OR_IN_PROGRESS.isSatisfiedBy(givenTodo, new NotAvailableExecutionContextProvider().provide());

        // Then
        assertThat(isSatisfiedBy).isEqualTo(expected);
    }
}
