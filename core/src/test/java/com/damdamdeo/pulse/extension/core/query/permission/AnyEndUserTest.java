package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
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
class AnyEndUserTest {

    @Mock
    Input input;

    @Mock
    Result<Projection> result;

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldReturnDecoratedResultWhenEndUserExecute() throws QueryException {
        // Given
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(permissionExecutionContext.isEndUser()).thenReturn(true);
        when(decorated.execute(input)).thenReturn(result);

        // When
        final Optional<Result<Projection>> executed = AnyEndUser.INSTANCE.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(decorated).execute(input),
                () -> verify(permissionExecutionContext).isEndUser());
    }

    @Test
    void shouldReturnEmptyWhenNotAnEndUser() throws QueryException {
        // Given
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(permissionExecutionContext.isEndUser()).thenReturn(false);

        // When
        final Optional<Result<Projection>> executed = AnyEndUser.INSTANCE.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.empty(), executed),
                () -> verify(decorated, never()).execute(input),
                () -> verify(permissionExecutionContext).isEndUser());
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given

        // When
        final int priority = AnyEndUser.INSTANCE.priority();

        // Then
        assertEquals(1, priority);
    }
}
