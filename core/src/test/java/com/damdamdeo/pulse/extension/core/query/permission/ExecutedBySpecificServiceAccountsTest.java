package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
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
class ExecutedBySpecificServiceAccountsTest {

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

    private static final ExecutedBy.ServiceAccount CHECKOUT = new ExecutedBy.ServiceAccount("checkout");
    private static final ExecutedBy.ServiceAccount PAYMENT = new ExecutedBy.ServiceAccount("payment");

    @Test
    void shouldReturnDecoratedResultWhenExecutedByIsOneOfSpecificServiceAccounts() throws QueryException {
        // Given
        final ExecutedBySpecificServiceAccounts permission = new ExecutedBySpecificServiceAccounts("checkout", "payment");
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(CHECKOUT);
        when(decorated.execute(input)).thenReturn(result);

        // When
        final Optional<Result<Projection>> executed = permission.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(executionContext).executedBy(),
                () -> verify(decorated).execute(input)
        );
    }

    @Test
    void shouldReturnDecoratedResultWhenExecutedByMatchesSecondSpecificServiceAccount() throws QueryException {
        // Given
        final ExecutedBySpecificServiceAccounts permission = new ExecutedBySpecificServiceAccounts("checkout", "payment");
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(PAYMENT);
        when(decorated.execute(input)).thenReturn(result);

        // When
        final Optional<Result<Projection>> executed = permission.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(executionContext).executedBy(),
                () -> verify(decorated).execute(input)
        );
    }

    @Test
    void shouldReturnEmptyWhenExecutedByIsNotOneOfSpecificServiceAccounts() throws QueryException {
        // Given
        final ExecutedBySpecificServiceAccounts permission = new ExecutedBySpecificServiceAccounts("checkout", "payment");
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        final ExecutedBy.ServiceAccount charlie = new ExecutedBy.ServiceAccount("charlie");
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(charlie);

        // When
        final Optional<Result<Projection>> executed = permission.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.empty(), executed),
                () -> verify(executionContext).executedBy(),
                () -> verifyNoInteractions(decorated)
        );
    }

    @Test
    void shouldReturnEmptyWhenSpecificServiceAccountsAreEmpty() throws QueryException {
        // Given
        final ExecutedBySpecificServiceAccounts permission = new ExecutedBySpecificServiceAccounts();
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(CHECKOUT);

        // When
        final Optional<Result<Projection>> executed = permission.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.empty(), executed),
                () -> verify(executionContext).executedBy(),
                () -> verifyNoInteractions(decorated)
        );
    }

    @Test
    void shouldNotMatchEndUserWithSameName() throws QueryException {
        // Given
        final ExecutedBySpecificServiceAccounts permission = new ExecutedBySpecificServiceAccounts("checkout");
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        final ExecutedBy.EndUser endUser = new ExecutedBy.EndUser(new Username("alice@mail.com"));

        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(endUser);

        // When
        final Optional<Result<Projection>> executed = permission.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.empty(), executed),
                () -> verify(executionContext).executedBy(),
                () -> verifyNoInteractions(decorated)
        );
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given
        final ExecutedBySpecificServiceAccounts permission = new ExecutedBySpecificServiceAccounts("checkout");

        // When
        final int priority = permission.priority();

        // Then
        assertEquals(4, priority);
    }
}
