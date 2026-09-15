package com.damdamdeo.pulse.extension.core.usecase.audience;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    AudienceExecutionContext audienceExecutionContext;

    private static final ExecutedBy.EndUser ALICE = new ExecutedBy.EndUser(new Username("alice@mail.com"));
    private static final ExecutedBy.EndUser BOB = new ExecutedBy.EndUser(new Username("bob@mail.com"));

    @Test
    void shouldAllowWhenExecutedByIsOneOfSpecificEndUsers() throws UseCaseException {
        // Given
        final ExecutedBySpecificEndUsers audience = new ExecutedBySpecificEndUsers(ALICE, BOB);
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(ALICE);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldAllowWhenExecutedByMatchesSecondSpecificEndUser() throws UseCaseException {
        // Given
        final ExecutedBySpecificEndUsers audience = new ExecutedBySpecificEndUsers(ALICE, BOB);
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(BOB);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldReturnFalseWhenExecutedByIsNotOneOfSpecificEndUsers() throws UseCaseException {
        // Given
        final ExecutedBySpecificEndUsers audience = new ExecutedBySpecificEndUsers(ALICE, BOB);
        final ExecutedBy.EndUser charlie = new ExecutedBy.EndUser(new Username("charlie@mail.com"));
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(charlie);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldReturnFalseWhenSpecificEndUsersAreEmpty() throws UseCaseException {
        // Given
        final ExecutedBySpecificEndUsers audience = new ExecutedBySpecificEndUsers();
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(ALICE);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldMatchEndUserUsingValue() throws UseCaseException {
        // Given
        final ExecutedBy.EndUser configuredEndUser = new ExecutedBy.EndUser(new Username("alice@mail.com"));
        final ExecutedBy.EndUser executionEndUser = new ExecutedBy.EndUser(new Username("alice@mail.com"));
        final ExecutedBySpecificEndUsers audience = new ExecutedBySpecificEndUsers(configuredEndUser);
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executionEndUser);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertTrue(allowed);
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given
        final ExecutedBySpecificEndUsers audience = new ExecutedBySpecificEndUsers(ALICE);

        // When
        final int priority = audience.priority();

        // Then
        assertEquals(5, priority);
    }
}
