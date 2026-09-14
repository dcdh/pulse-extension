package com.damdamdeo.pulse.extension.core.query.audience;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
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
class ExecutedBySpecificEndUsersTest {

    @Mock
    Input input;

    @Mock
    Result<Projection> result;

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    AudienceExecutionContext audienceExecutionContext;

    private static final ExecutedBy.EndUser ALICE = new ExecutedBy.EndUser(new Username("alice@mail.com"));
    private static final ExecutedBy.EndUser BOB = new ExecutedBy.EndUser(new Username("bob@mail.com"));

    @Test
    void shouldReturnDecoratedResultWhenExecutedByIsOneOfSpecificEndUsers() throws QueryException {
        // Given
        final ExecutedBySpecificEndUsers audience = new ExecutedBySpecificEndUsers(ALICE, BOB);
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(ALICE);
        when(decorated.execute(input)).thenReturn(result);

        // When
        final Optional<Result<Projection>> executed = audience.execute(input, decorated, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(decorated).execute(input),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldReturnDecoratedResultWhenExecutedByMatchesSecondSpecificEndUser() throws QueryException {
        // Given
        final ExecutedBySpecificEndUsers audience = new ExecutedBySpecificEndUsers(ALICE, BOB);
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(BOB);
        when(decorated.execute(input)).thenReturn(result);

        // When
        final Optional<Result<Projection>> executed = audience.execute(input, decorated, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(decorated).execute(input),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldReturnEmptyWhenExecutedByIsNotOneOfSpecificEndUsers() throws QueryException {
        // Given
        final ExecutedBySpecificEndUsers audience = new ExecutedBySpecificEndUsers(ALICE, BOB);
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        final ExecutedBy.EndUser charlie = new ExecutedBy.EndUser(new Username("charlie@mail.com"));
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(charlie);

        // When
        final Optional<Result<Projection>> executed = audience.execute(input, decorated, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.empty(), executed),
                () -> verify(executionContext).executedBy(),
                () -> verifyNoInteractions(decorated)
        );
    }

    @Test
    void shouldReturnEmptyWhenSpecificEndUsersAreEmpty() throws QueryException {
        // Given
        final ExecutedBySpecificEndUsers audience = new ExecutedBySpecificEndUsers();
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(ALICE);

        // When
        final Optional<Result<Projection>> executed = audience.execute(input, decorated, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.empty(), executed),
                () -> verify(executionContext).executedBy(),
                () -> verifyNoInteractions(decorated)
        );
    }

    @Test
    void shouldMatchEndUserUsingValue() throws QueryException {
        // Given
        final ExecutedBy.EndUser configuredEndUser = new ExecutedBy.EndUser(new Username("alice@mail.com"));
        final ExecutedBy.EndUser executionEndUser = new ExecutedBy.EndUser(new Username("alice@mail.com"));
        final ExecutedBySpecificEndUsers audience = new ExecutedBySpecificEndUsers(configuredEndUser);
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executionEndUser);
        when(decorated.execute(input)).thenReturn(result);

        // When
        final Optional<Result<Projection>> executed = audience.execute(input, decorated, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(decorated).execute(input)
        );
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given
        final ExecutedBySpecificEndUsers audience =
                new ExecutedBySpecificEndUsers(ALICE);

        // When
        final int priority = audience.priority();

        // Then
        assertEquals(5, priority);
    }
}
