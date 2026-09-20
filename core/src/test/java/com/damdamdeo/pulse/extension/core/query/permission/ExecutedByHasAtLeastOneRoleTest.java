package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
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
class ExecutedByHasAtLeastOneRoleTest {

    @Mock
    Input input;

    @Mock
    Result<Projection> result;

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldReturnDecoratedResultWhenExecutedByHasOneOfTheRequiredRoles() throws QueryException {
        // Given
        final ExecutedByHasAtLeastOneRole permission = new ExecutedByHasAtLeastOneRole("ADMIN", "USER");
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(true);
        when(decorated.execute(input)).thenReturn(result);

        // When
        final Optional<Result<Projection>> executed = permission.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(decorated).execute(input),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER"),
                () -> verifyNoMoreInteractions(decorated)
        );
    }

    @Test
    void shouldReturnDecoratedResultWhenExecutedByHasFirstRequiredRole() throws QueryException {
        // Given
        final ExecutedByHasAtLeastOneRole permission = new ExecutedByHasAtLeastOneRole("ADMIN", "USER");
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.hasRole("ADMIN")).thenReturn(true);
        when(decorated.execute(input)).thenReturn(result);

        // When
        final Optional<Result<Projection>> executed = permission.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(decorated).execute(input),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext, never()).hasRole("USER")
        );
    }

    @Test
    void shouldReturnEmptyWhenExecutedByHasNoneOfTheRequiredRoles() throws QueryException {
        // Given
        final ExecutedByHasAtLeastOneRole permission = new ExecutedByHasAtLeastOneRole("ADMIN", "USER");
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(false);

        // When
        final Optional<Result<Projection>> executed = permission.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.empty(), executed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER"),
                () -> verifyNoInteractions(decorated)
        );
    }

    @Test
    void shouldReturnEmptyWhenRequiredRolesAreEmpty() throws QueryException {
        // Given
        final ExecutedByHasAtLeastOneRole permission = new ExecutedByHasAtLeastOneRole();
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);

        // When
        final Optional<Result<Projection>> executed = permission.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.empty(), executed),
                () -> verifyNoInteractions(executionContext),
                () -> verifyNoInteractions(decorated)
        );
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given
        final ExecutedByHasAtLeastOneRole permission = new ExecutedByHasAtLeastOneRole("ADMIN");

        // When
        final int priority = permission.priority();

        // Then
        assertEquals(5, priority);
    }
}
