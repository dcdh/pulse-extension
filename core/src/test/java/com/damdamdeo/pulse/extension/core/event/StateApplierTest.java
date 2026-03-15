package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.NotAvailableExecutionContextProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class StateApplierTest {

    ExecutionContextProvider executionContextProvider = new NotAvailableExecutionContextProvider();

    @Test
    void shouldApplyEvents() {
        // Given
        final List<ExecutedByEvent> givenExecutedByEvents = List.of(
                new ExecutedByEvent(new NewTodoCreated("lorem ipsum"), executionContextProvider.provide().executedBy()),
                new ExecutedByEvent(new TodoMarkedAsDone(), executionContextProvider.provide().executedBy()));

        // When
        final StateApplier<Todo, TodoId> todoStateApplier = new StateApplier<>(
                new ReflectionAggregateRootInstanceCreator(), executionContextProvider, givenExecutedByEvents, Todo.class,
                TodoId.class, new TodoId("Damien", 0L));

        // Then
        assertAll(
                () -> assertThat(todoStateApplier.aggregate().id()).isEqualTo(new TodoId("Damien", 0L)),
                () -> assertThat(todoStateApplier.aggregate().description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoStateApplier.aggregate().status()).isEqualTo(Status.DONE));
    }

    @Test
    void shouldNewVersionizedEventsIsEmptyWhenCommandsNotCalled() {
        // Given
        final List<ExecutedByEvent> givenExecutedByEvents = List.of(
                new ExecutedByEvent(new NewTodoCreated("lorem ipsum"), executionContextProvider.provide().executedBy()),
                new ExecutedByEvent(new TodoMarkedAsDone(), executionContextProvider.provide().executedBy()));

        // When
        final StateApplier<Todo, TodoId> todoStateApplier = new StateApplier<>(
                new ReflectionAggregateRootInstanceCreator(), executionContextProvider, givenExecutedByEvents, Todo.class,
                TodoId.class, new TodoId("Damien", 0L));

        // Then
        assertThat(todoStateApplier.getNewEvents()).isEmpty();
    }

    @Test
    void shouldIncrementOnAppendANewEvent() {
        // Given
        final List<ExecutedByEvent> givenEvents = List.of(
                new ExecutedByEvent(new NewTodoCreated("lorem ipsum"), executionContextProvider.provide().executedBy())
        );
        final StateApplier<Todo, TodoId> todoStateApplier = new StateApplier<>(
                new ReflectionAggregateRootInstanceCreator(), executionContextProvider, givenEvents, Todo.class,
                TodoId.class, new TodoId("Damien", 0L));

        // When
        todoStateApplier.append(new TodoMarkedAsDone());

        // Then
        assertThat(todoStateApplier.getNewEvents()).containsExactly(
                new VersionizedEvent(new AggregateVersion(1),
                        new ExecutedByEvent(new TodoMarkedAsDone(), executionContextProvider.provide().executedBy()))
        );
    }
}
