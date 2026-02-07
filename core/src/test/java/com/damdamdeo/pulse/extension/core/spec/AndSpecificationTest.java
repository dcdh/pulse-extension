package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.executedby.NotAvailableExecutionContextProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AndSpecificationTest {

    private static final Specification<Todo> IMPORTANT_AND_IN_PROGRESS = new TodoIsImportantSpec()
            .and(new TodoIsInProgressSpec());

    @ParameterizedTest
    @CsvSource(textBlock = """
            # givenStatus, givenImportant, expected
            IN_PROGRESS, true, true
            IN_PROGRESS, false, false
            DONE, true, false
            DONE, false, false
            """
    )
    void shouldValidateSpecification(final Status givenStatus, final boolean givenImportant, final boolean expected) {
        // Given
        final Todo givenTodo = new Todo(new TodoId("Damien", 0L), "lorem", givenStatus, givenImportant);

        // When
        final boolean isSatisfiedBy = IMPORTANT_AND_IN_PROGRESS.isSatisfiedBy(givenTodo, new NotAvailableExecutionContextProvider().provide());

        // Then
        assertThat(isSatisfiedBy).isEqualTo(expected);
    }
}
