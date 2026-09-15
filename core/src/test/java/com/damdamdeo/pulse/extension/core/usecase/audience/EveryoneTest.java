package com.damdamdeo.pulse.extension.core.usecase.audience;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class EveryoneTest {

    @Mock
    AudienceExecutionContext audienceExecutionContext;

    @Test
    void shouldReturnDecoratedResultWhenExecute() throws UseCaseException {
        // Given

        // When
        final boolean allowed = Everyone.INSTANCE.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertThat(allowed).isTrue();
    }

    @Test
    void shouldHaveHighestVisibilityPriority() {
        // Given

        // When
        final int priority = Everyone.INSTANCE.priority();

        // Then
        assertEquals(0, priority);
    }
}
