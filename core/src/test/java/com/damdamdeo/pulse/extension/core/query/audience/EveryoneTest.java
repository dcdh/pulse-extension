package com.damdamdeo.pulse.extension.core.query.audience;

import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.query.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EveryoneTest {

    @Mock
    Input input;

    @Mock
    Result<Projection> result;

    @Mock
    AudienceExecutionContext audienceExecutionContext;

    @Test
    void shouldReturnDecoratedResultWhenExecute() throws QueryException {
        // Given
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(decorated.execute(input)).thenReturn(result);

        // When
        final Optional<Result<Projection>> executed = Everyone.INSTANCE.execute(input, decorated, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(decorated).execute(input));
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
