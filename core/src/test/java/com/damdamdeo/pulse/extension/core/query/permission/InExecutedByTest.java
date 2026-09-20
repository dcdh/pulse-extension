package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.permission.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InExecutedByTest {

    @Mock
    Input input;

    @Mock
    Result<Projection> result;

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    AggregateIdDecomposer aggregateIdDecomposer;

    @Mock
    ExecutedByResolver executedByResolver;

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldReturnDecoratedResultWhenExecutedByIsEligible() throws QueryException, UnableToResolveException {
        // Given
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        final Set<AggregateId> aggregateIds = Set.of(TodoId.USER_1_TODO_1);
        final Set<AggregateId> uncompoundedAggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1, TodoId.USER_1_TODO_1);
        final Set<ExecutedBy> executedByEligibles = Set.of(new ExecutedBy.EndUser(new Username("alice@mail.com")));
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.aggregateIdDecomposer()).thenReturn(aggregateIdDecomposer);
        when(permissionExecutionContext.executedByResolver()).thenReturn(executedByResolver);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(new ExecutedBy.EndUser(new Username("alice@mail.com")));
        when(decorated.execute(input)).thenReturn(result);
        when(result.aggregateIds()).thenReturn(aggregateIds);
        when(aggregateIdDecomposer.unCompound(aggregateIds)).thenReturn(uncompoundedAggregateIds);
        when(executedByResolver.resolve(uncompoundedAggregateIds)).thenReturn(executedByEligibles);

        // When
        final Optional<Result<Projection>> executed = InExecutedBy.INSTANCE.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(decorated).execute(input),
                () -> verify(aggregateIdDecomposer).unCompound(aggregateIds),
                () -> verify(executedByResolver).resolve(uncompoundedAggregateIds),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldReturnEmptyWhenExecutedByIsNotEligible() throws QueryException, UnableToResolveException {
        // Given
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        final ExecutedBy anotherExecutedBy = new ExecutedBy.EndUser(new Username("bob@mail.com"));
        final Set<AggregateId> aggregateIds = Set.of(TodoId.USER_1_TODO_1);
        final Set<AggregateId> uncompoundedAggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1, TodoId.USER_1_TODO_1);
        final Set<ExecutedBy> executedByEligibles = Set.of(anotherExecutedBy);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.aggregateIdDecomposer()).thenReturn(aggregateIdDecomposer);
        when(permissionExecutionContext.executedByResolver()).thenReturn(executedByResolver);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(new ExecutedBy.EndUser(new Username("alice@mail.com")));
        when(decorated.execute(input)).thenReturn(result);
        when(result.aggregateIds()).thenReturn(aggregateIds);
        when(aggregateIdDecomposer.unCompound(aggregateIds)).thenReturn(uncompoundedAggregateIds);
        when(executedByResolver.resolve(uncompoundedAggregateIds)).thenReturn(executedByEligibles);

        // When
        final Optional<Result<Projection>> executed = InExecutedBy.INSTANCE.execute(
                input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.empty(), executed),
                () -> verify(decorated).execute(input),
                () -> verify(aggregateIdDecomposer).unCompound(aggregateIds),
                () -> verify(executedByResolver).resolve(uncompoundedAggregateIds),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldResolveUncompoundedAggregateIds() throws QueryException, UnableToResolveException {
        // Given
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        final Set<AggregateId> aggregateIds = Set.of(TodoId.USER_1_TODO_1);
        final Set<AggregateId> uncompoundedAggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1, TodoId.USER_1_TODO_1);
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.aggregateIdDecomposer()).thenReturn(aggregateIdDecomposer);
        when(permissionExecutionContext.executedByResolver()).thenReturn(executedByResolver);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(new ExecutedBy.EndUser(new Username("alice@mail.com")));
        when(decorated.execute(input)).thenReturn(result);
        when(result.aggregateIds()).thenReturn(aggregateIds);
        when(aggregateIdDecomposer.unCompound(aggregateIds)).thenReturn(uncompoundedAggregateIds);
        when(executedByResolver.resolve(uncompoundedAggregateIds)).thenReturn(Set.of(new ExecutedBy.EndUser(new Username("alice@mail.com"))));

        // When
        InExecutedBy.INSTANCE.execute(input, decorated, permissionExecutionContext);

        // Then
        assertAll(
                () -> verify(aggregateIdDecomposer).unCompound(aggregateIds),
                () -> verify(executedByResolver).resolve(uncompoundedAggregateIds)
        );
    }

    @Test
    void shouldThrowQueryExceptionWhenExecutedByCannotBeResolved() throws QueryException, UnableToResolveException {
        // Given
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        final Set<AggregateId> aggregateIds = Set.of(TodoId.USER_1_TODO_1);
        final Set<AggregateId> uncompoundedAggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1, TodoId.USER_1_TODO_1);
        final UnableToResolveException cause = new UnableToResolveException(new RuntimeException("Unable to resolve executed by"));
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.aggregateIdDecomposer()).thenReturn(aggregateIdDecomposer);
        when(permissionExecutionContext.executedByResolver()).thenReturn(executedByResolver);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(decorated.execute(input)).thenReturn(result);
        when(result.aggregateIds()).thenReturn(aggregateIds);
        when(aggregateIdDecomposer.unCompound(aggregateIds)).thenReturn(uncompoundedAggregateIds);
        when(executedByResolver.resolve(uncompoundedAggregateIds)).thenThrow(cause);

        // When
        final QueryException exception = Assertions.assertThrows(
                QueryException.class,
                () -> InExecutedBy.INSTANCE.execute(input, decorated, permissionExecutionContext));

        // Then
        assertAll(
                () -> assertEquals(QueryExceptionCode.INFRASTRUCTURE_FAILURE, exception.queryExceptionCode()),
                () -> assertSame(cause, exception.getCause())
        );
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given

        // When
        final int priority = InExecutedBy.INSTANCE.priority();

        // Then
        assertEquals(2, priority);
    }
}
