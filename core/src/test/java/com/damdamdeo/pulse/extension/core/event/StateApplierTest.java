package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class StateApplierTest {

    @Test
    void shouldFailToApplyOnDifferentAggregateIdentifier() {
        assertThatThrownBy(() -> new StateApplier<>(
                new ReflectionAggregateRootInstanceCreator(),
                List.of(
                        new NewTodoCreated(new TodoId("Damien", 0L), "lorem ipsum"),
                        new TodoMarkedAsDone(new TodoId("Damien", 1L))
                ),
                Todo.class
        ))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Applying event on an aggregate with different id.");
    }

    @Test
    void shouldApplyEvents() {
        // Given
        final List<Event<TodoId>> givenEvents = List.of(
                new NewTodoCreated(new TodoId("Damien", 0L), "lorem ipsum"),
                new TodoMarkedAsDone(new TodoId("Damien", 0L))
        );

        // When
        final StateApplier<Todo, TodoId> todoStateApplier = new StateApplier<>(
                new ReflectionAggregateRootInstanceCreator(), givenEvents, Todo.class);

        // Then
        assertAll(
                () -> assertThat(todoStateApplier.aggregate().id()).isEqualTo(new TodoId("Damien", 0L)),
                () -> assertThat(todoStateApplier.aggregate().description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoStateApplier.aggregate().status()).isEqualTo(Status.DONE));
    }

    @Test
    void shouldNewVersionizedEventsIsEmptyWhenCommandsNotCalled() {
        // Given
        final List<Event<TodoId>> givenEvents = List.of(
                new NewTodoCreated(new TodoId("Damien", 0L), "lorem ipsum"),
                new TodoMarkedAsDone(new TodoId("Damien", 0L))
        );

        // When
        final StateApplier<Todo, TodoId> todoStateApplier = new StateApplier<>(
                new ReflectionAggregateRootInstanceCreator(), givenEvents, Todo.class);

        // Then
        assertThat(todoStateApplier.getNewEvents()).isEmpty();
    }

    @Test
    void shouldIncrementOnAppendANewEvent() {
        // Given
        final List<Event<TodoId>> givenEvents = List.of(
                new NewTodoCreated(new TodoId("Damien", 0L), "lorem ipsum")
        );
        final StateApplier<Todo, TodoId> todoStateApplier = new StateApplier<>(
                new ReflectionAggregateRootInstanceCreator(), givenEvents, Todo.class);

        // When
        todoStateApplier.append(new TodoMarkedAsDone(new TodoId("Damien", 0L)));

        // Then
        assertThat(todoStateApplier.getNewEvents()).containsExactly(
                new VersionizedEvent<>(new AggregateVersion(1), new TodoMarkedAsDone(new TodoId("Damien", 0L)))
        );
    }
}
