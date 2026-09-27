package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.Input;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutedByHasAtLeastOneRoleTest {

    @Mock
    Input input;

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldAllowWhenExecutedByHasOneOfTheRequiredRoles() throws QueryException {
        // Given
        final ExecutedByHasAtLeastOneRole<TodoId> permission = new ExecutedByHasAtLeastOneRole<>("ADMIN", "USER");
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(true);

        // When
        final boolean allowed = permission.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER"),
                () -> verify(executionContextProvider).provide()
        );
    }

    @Test
    void shouldAllowWhenExecutedByHasFirstRequiredRole() throws QueryException {
        // Given
        final ExecutedByHasAtLeastOneRole<TodoId> permission = new ExecutedByHasAtLeastOneRole<>("ADMIN", "USER");
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.hasRole("ADMIN")).thenReturn(true);

        // When
        final boolean allowed = permission.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext, never()).hasRole("USER"),
                () -> verify(executionContextProvider).provide()
        );
    }

    @Test
    void shouldDisallowWhenExecutedByHasNoneOfTheRequiredRoles() throws QueryException {
        // Given
        final ExecutedByHasAtLeastOneRole<TodoId> permission = new ExecutedByHasAtLeastOneRole<>("ADMIN", "USER");
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(false);

        // When
        final boolean allowed = permission.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER"),
                () -> verify(executionContextProvider).provide()
        );
    }

    @Test
    void shouldDisallowWhenRequiredRolesAreEmpty() throws QueryException {
        // Given
        final ExecutedByHasAtLeastOneRole<TodoId> permission = new ExecutedByHasAtLeastOneRole<>();
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);

        // When
        final boolean allowed = permission.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verifyNoInteractions(executionContext),
                () -> verify(executionContextProvider).provide()
        );
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given
        final ExecutedByHasAtLeastOneRole<TodoId> permission = new ExecutedByHasAtLeastOneRole<>("ADMIN");

        // When
        final int priority = permission.priority();

        // Then
        assertEquals(5, priority);
    }
}
