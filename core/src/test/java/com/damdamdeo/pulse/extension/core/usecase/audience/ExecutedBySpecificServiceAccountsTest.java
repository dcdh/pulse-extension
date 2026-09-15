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
class ExecutedBySpecificServiceAccountsTest {

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    AudienceExecutionContext audienceExecutionContext;

    private static final ExecutedBy.ServiceAccount CHECKOUT = new ExecutedBy.ServiceAccount("checkout");
    private static final ExecutedBy.ServiceAccount PAYMENT = new ExecutedBy.ServiceAccount("payment");

    @Test
    void shouldAllowWhenExecutedByIsOneOfSpecificServiceAccounts() throws UseCaseException {
        // Given
        final ExecutedBySpecificServiceAccounts audience = new ExecutedBySpecificServiceAccounts("checkout", "payment");
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(CHECKOUT);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldAllowWhenExecutedByMatchesSecondSpecificServiceAccount() throws UseCaseException {
        // Given
        final ExecutedBySpecificServiceAccounts audience = new ExecutedBySpecificServiceAccounts("checkout", "payment");
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(PAYMENT);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldReturnFalseWhenExecutedByIsNotOneOfSpecificServiceAccounts() throws UseCaseException {
        // Given
        final ExecutedBySpecificServiceAccounts audience = new ExecutedBySpecificServiceAccounts("checkout", "payment");
        final ExecutedBy.ServiceAccount charlie = new ExecutedBy.ServiceAccount("charlie");
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
    void shouldReturnFalseWhenSpecificServiceAccountsAreEmpty() throws UseCaseException {
        // Given
        final ExecutedBySpecificServiceAccounts audience = new ExecutedBySpecificServiceAccounts();
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(CHECKOUT);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldNotMatchEndUserWithSameName() throws UseCaseException {
        // Given
        final ExecutedBySpecificServiceAccounts audience = new ExecutedBySpecificServiceAccounts("checkout");
        final ExecutedBy.EndUser endUser = new ExecutedBy.EndUser(new Username("alice@mail.com"));

        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(endUser);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given
        final ExecutedBySpecificServiceAccounts audience = new ExecutedBySpecificServiceAccounts("checkout");

        // When
        final int priority = audience.priority();

        // Then
        assertEquals(3, priority);
    }
}
