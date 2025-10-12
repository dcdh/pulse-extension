package com.damdamdeo.pulse.extension.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class StateApplierTest {

    @Test
    void shouldFailToApplyOnDifferentAggregateIdentifier() {
        assertThatThrownBy(() -> new TodoStateApplier(
                List.of(
                        new NewTodoCreated(new TodoId(new UUID(0,0)), "lorem ipsum"),
                        new TodoMarkedAsDone(new TodoId(new UUID(0,1)))
                )
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Applying event on an aggregate with different id.");
    }

    @Test
    void shouldApplyEvents() {
        // Given
        final List<Event<TodoId>> givenEvents = List.of(
                new NewTodoCreated(new TodoId(new UUID(0,0)), "lorem ipsum"),
                new TodoMarkedAsDone(new TodoId(new UUID(0,0)))
        );

        // When
        final StateApplier<Todo, TodoId> todoStateApplier = new TodoStateApplier(givenEvents);

        // Then
        assertAll(
                () -> assertThat(todoStateApplier.aggregate().id()).isEqualTo(new TodoId(new UUID(0,0))),
                () -> assertThat(todoStateApplier.aggregate().description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoStateApplier.aggregate().status()).isEqualTo(Status.DONE));
    }

    @Test
    void shouldNewVersionizedEventsIsEmptyWhenCommandsNotCalled() {
        // Given
        final List<Event<TodoId>> givenEvents = List.of(
                new NewTodoCreated(new TodoId(new UUID(0,0)), "lorem ipsum"),
                new TodoMarkedAsDone(new TodoId(new UUID(0,0)))
        );

        // When
        final StateApplier<Todo, TodoId> todoStateApplier = new TodoStateApplier(givenEvents);

        // Then
        assertThat(todoStateApplier.getNewEvents()).isEmpty();
    }

    @Test
    void shouldIncrementOnAppendANewEvent() {
        // Given
        final List<Event<TodoId>> givenEvents = List.of(
                new NewTodoCreated(new TodoId(new UUID(0,0)), "lorem ipsum")
        );
        final StateApplier<Todo, TodoId> todoStateApplier = new TodoStateApplier(givenEvents);

        // When
        todoStateApplier.append(new TodoMarkedAsDone(new TodoId(new UUID(0,0))));

        // Then
        assertThat(todoStateApplier.getNewEvents()).containsExactly(
                new VersionizedEvent<>(new AggregateVersion(1), new TodoMarkedAsDone(new TodoId(new UUID(0,0))))
        );
    }
}
