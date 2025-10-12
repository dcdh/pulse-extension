package com.damdamdeo.pulse.extension.runtime;

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
        final TodoStateApplier todoStateApplier = new TodoStateApplier(givenEvents);

        // Then
        assertAll(
                () -> assertThat(todoStateApplier.aggregate().id()).isEqualTo(new TodoId(new UUID(0,0))),
                () -> assertThat(todoStateApplier.aggregate().description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoStateApplier.aggregate().status()).isEqualTo(Status.DONE));
    }

    static class TodoStateApplier extends StateApplier<Todo, TodoId> {

        public TodoStateApplier(final List<Event<TodoId>> events) {
            super(new ReflectionAggregateRootInstanceCreator(), events);
        }

        @Override
        Class<Todo> getAggregateClass() {
            return Todo.class;
        }
    }
}
