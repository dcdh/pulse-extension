package com.damdamdeo.pulse.extension.core.usecase.audience;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.AggregateIdDecomposer;
import com.damdamdeo.pulse.extension.core.query.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.query.UnableToResolveException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InExecutedByTest {

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    AggregateIdDecomposer aggregateIdDecomposer;

    @Mock
    ExecutedByResolver executedByResolver;

    @Mock
    AudienceExecutionContext audienceExecutionContext;

    @Test
    void shouldAllowWhenExecutedByIsEligible() throws UseCaseException, UnableToResolveException {
        // Given
        final Set<AggregateId> aggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1);
        final Set<AggregateId> uncompoundedAggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1, TodoId.USER_1_TODO_1);
        final Set<ExecutedBy> executedByEligibles = Set.of(new ExecutedBy.EndUser(new Username("alice@mail.com")));
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(audienceExecutionContext.aggregateIdDecomposer()).thenReturn(aggregateIdDecomposer);
        when(audienceExecutionContext.executedByResolver()).thenReturn(executedByResolver);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(new ExecutedBy.EndUser(new Username("alice@mail.com")));
        when(aggregateIdDecomposer.unCompound(aggregateIds)).thenReturn(uncompoundedAggregateIds);
        when(executedByResolver.resolve(uncompoundedAggregateIds)).thenReturn(executedByEligibles);

        // When
        final boolean allowed = InExecutedBy.INSTANCE.allow(TodoChecklistId.USER_1_TODO_1_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(aggregateIdDecomposer).unCompound(aggregateIds),
                () -> verify(executedByResolver).resolve(uncompoundedAggregateIds),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldReturnFalseWhenExecutedByIsNotEligible() throws UseCaseException, UnableToResolveException {
        // Given
        final ExecutedBy anotherExecutedBy = new ExecutedBy.EndUser(new Username("bob@mail.com"));
        final Set<AggregateId> aggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1);
        final Set<AggregateId> uncompoundedAggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1, TodoId.USER_1_TODO_1);
        final Set<ExecutedBy> executedByEligibles = Set.of(anotherExecutedBy);
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(audienceExecutionContext.aggregateIdDecomposer()).thenReturn(aggregateIdDecomposer);
        when(audienceExecutionContext.executedByResolver()).thenReturn(executedByResolver);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(new ExecutedBy.EndUser(new Username("alice@mail.com")));
        when(aggregateIdDecomposer.unCompound(aggregateIds)).thenReturn(uncompoundedAggregateIds);
        when(executedByResolver.resolve(uncompoundedAggregateIds)).thenReturn(executedByEligibles);

        // When
        final boolean allowed = InExecutedBy.INSTANCE.allow(TodoChecklistId.USER_1_TODO_1_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(aggregateIdDecomposer).unCompound(aggregateIds),
                () -> verify(executedByResolver).resolve(uncompoundedAggregateIds),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldResolveUncompoundedAggregateIds() throws UseCaseException, UnableToResolveException {
        // Given
        final Set<AggregateId> aggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1);
        final Set<AggregateId> uncompoundedAggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1, TodoId.USER_1_TODO_1);
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(audienceExecutionContext.aggregateIdDecomposer()).thenReturn(aggregateIdDecomposer);
        when(audienceExecutionContext.executedByResolver()).thenReturn(executedByResolver);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(new ExecutedBy.EndUser(new Username("alice@mail.com")));
        when(aggregateIdDecomposer.unCompound(aggregateIds)).thenReturn(uncompoundedAggregateIds);
        when(executedByResolver.resolve(uncompoundedAggregateIds)).thenReturn(Set.of(new ExecutedBy.EndUser(new Username("alice@mail.com"))));

        // When
        InExecutedBy.INSTANCE.allow(TodoChecklistId.USER_1_TODO_1_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> verify(aggregateIdDecomposer).unCompound(aggregateIds),
                () -> verify(executedByResolver).resolve(uncompoundedAggregateIds)
        );
    }

    @Test
    void shouldThrowQueryExceptionWhenExecutedByCannotBeResolved() throws UnableToResolveException {
        // Given
        final Set<AggregateId> aggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1);
        final Set<AggregateId> uncompoundedAggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1, TodoId.USER_1_TODO_1);
        final UnableToResolveException cause = new UnableToResolveException(new RuntimeException("Unable to resolve executed by"));
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(audienceExecutionContext.aggregateIdDecomposer()).thenReturn(aggregateIdDecomposer);
        when(audienceExecutionContext.executedByResolver()).thenReturn(executedByResolver);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(aggregateIdDecomposer.unCompound(aggregateIds)).thenReturn(uncompoundedAggregateIds);
        when(executedByResolver.resolve(uncompoundedAggregateIds)).thenThrow(cause);

        // When
        final UseCaseException exception = assertThrows(
                UseCaseException.class,
                () -> InExecutedBy.INSTANCE.allow(TodoChecklistId.USER_1_TODO_1_1, audienceExecutionContext));

        // Then
        assertSame(cause, exception.getCause());
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
