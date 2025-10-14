package com.damdamdeo.pulse.extension.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TodoCommandHandlerTest {

    TodoCommandHandler todoCommandHandler;

    @Mock
    EventRepository<Todo, TodoId> eventRepository;

    @BeforeEach
    public void setUp() {
        todoCommandHandler = new TodoCommandHandler(eventRepository, new NoOpTransaction());
    }

    @Test
    void shouldCreateTodo() {
        // Given
        final CreateTodo createTodo = new CreateTodo(TodoId.from(new UUID(0, 0)), "lorem ipsum");
        doReturn(List.of()).when(eventRepository).loadOrderByVersionASC(TodoId.from(new UUID(0, 0)));

        // When
        final Todo todoCreated = todoCommandHandler.handle(createTodo);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(TodoId.from(new UUID(0, 0))),
                () -> assertThat(todoCreated.description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoCreated.status()).isEqualTo(Status.IN_PROGRESS),
                () -> verify(eventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                new AggregateVersion(0),
                                new NewTodoCreated(TodoId.from(new UUID(0, 0)), "lorem ipsum")))
                )
        );
    }

    @Test
    void shouldMarkTodoAsDone() {
        // Given
        final MarkTodoAsDone markTodoAsDone = new MarkTodoAsDone(TodoId.from(new UUID(0, 0)));
        doReturn(List.of(new NewTodoCreated(TodoId.from(new UUID(0, 0)), "lorem ipsum")))
                .when(eventRepository).loadOrderByVersionASC(TodoId.from(new UUID(0, 0)));

        // When
        final Todo todoMarkedAsDone = todoCommandHandler.handle(markTodoAsDone);

        // Then
        assertAll(
                () -> assertThat(todoMarkedAsDone.id()).isEqualTo(TodoId.from(new UUID(0, 0))),
                () -> assertThat(todoMarkedAsDone.description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoMarkedAsDone.status()).isEqualTo(Status.DONE),
                () -> verify(eventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                new AggregateVersion(1),
                                new TodoMarkedAsDone(TodoId.from(new UUID(0, 0)))))
                )
        );
    }
}
