package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutedBySpecificEndUsersTest {

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    private static final ExecutedBy.EndUser ALICE = new ExecutedBy.EndUser(new Username("alice@mail.com"));
    private static final ExecutedBy.EndUser BOB = new ExecutedBy.EndUser(new Username("bob@mail.com"));

    @Test
    void shouldAllowWhenExecutedByIsOneOfSpecificEndUsers() throws QueryException {
        // Given
        final ExecutedBySpecificEndUsers<TodoId> permission = new ExecutedBySpecificEndUsers<>(ALICE, BOB);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(ALICE);

        // When
        final boolean allowed = permission.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).executedBy(),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldAllowWhenExecutedByMatchesSecondSpecificEndUser() throws QueryException {
        // Given
        final ExecutedBySpecificEndUsers<TodoId> permission = new ExecutedBySpecificEndUsers<>(ALICE, BOB);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(BOB);

        // When
        final boolean allowed = permission.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).executedBy(),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldDisallowWhenExecutedByIsNotOneOfSpecificEndUsers() throws QueryException {
        // Given
        final ExecutedBySpecificEndUsers<TodoId> permission = new ExecutedBySpecificEndUsers<>(ALICE, BOB);
        final ExecutedBy.EndUser charlie = new ExecutedBy.EndUser(new Username("charlie@mail.com"));
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(charlie);

        // When
        final boolean allowed = permission.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(executionContext).executedBy(),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldDisallowWhenSpecificEndUsersAreEmpty() throws QueryException {
        // Given
        final ExecutedBySpecificEndUsers<TodoId> permission = new ExecutedBySpecificEndUsers<>();
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(ALICE);

        // When
        final boolean allowed = permission.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(executionContext).executedBy(),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldMatchEndUserUsingValue() throws QueryException {
        // Given
        final ExecutedBy.EndUser configuredEndUser = new ExecutedBy.EndUser(new Username("alice@mail.com"));
        final ExecutedBy.EndUser executionEndUser = new ExecutedBy.EndUser(new Username("alice@mail.com"));
        final ExecutedBySpecificEndUsers<TodoId> permission = new ExecutedBySpecificEndUsers<>(configuredEndUser);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executionEndUser);

        // When
        final boolean allowed = permission.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given
        final ExecutedBySpecificEndUsers<TodoId> permission = new ExecutedBySpecificEndUsers<>(ALICE);

        // When
        final int priority = permission.priority();

        // Then
        assertEquals(6, priority);
    }
}
