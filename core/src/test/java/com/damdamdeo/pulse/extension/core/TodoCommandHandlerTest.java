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
        final CreateTodo givenCreateTodo = new CreateTodo(TodoId.from(new UUID(0, 0)), "lorem ipsum");
        doReturn(List.of()).when(eventRepository).loadOrderByVersionASC(TodoId.from(new UUID(0, 0)));

        // When
        final Todo todoCreated = todoCommandHandler.handle(givenCreateTodo);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(TodoId.from(new UUID(0, 0))),
                () -> assertThat(todoCreated.description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoCreated.status()).isEqualTo(Status.IN_PROGRESS),
                () -> assertThat(todoCreated.important()).isEqualTo(Boolean.FALSE),
                () -> verify(eventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                new AggregateVersion(0),
                                new NewTodoCreated(TodoId.from(new UUID(0, 0)), "lorem ipsum")))
                )
        );
    }

    @Test
    void shouldClassifieAsImportant() {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo(TodoId.from(new UUID(0, 0)), "IMPORTANT lorem ipsum");
        doReturn(List.of()).when(eventRepository).loadOrderByVersionASC(TodoId.from(new UUID(0, 0)));

        // When
        final Todo todoCreated = todoCommandHandler.handle(givenCreateTodo);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(TodoId.from(new UUID(0, 0))),
                () -> assertThat(todoCreated.description()).isEqualTo("IMPORTANT lorem ipsum"),
                () -> assertThat(todoCreated.status()).isEqualTo(Status.IN_PROGRESS),
                () -> assertThat(todoCreated.important()).isEqualTo(Boolean.TRUE),
                () -> verify(eventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                        new AggregateVersion(0),
                                        new NewTodoCreated(TodoId.from(new UUID(0, 0)), "IMPORTANT lorem ipsum")),
                                new VersionizedEvent<>(
                                        new AggregateVersion(1),
                                        new ClassifiedAsImportant(TodoId.from(new UUID(0, 0)))))
                )
        );
    }

    @Test
    void shouldMarkTodoAsDone() {
        // Given
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(TodoId.from(new UUID(0, 0)));
        doReturn(List.of(new NewTodoCreated(TodoId.from(new UUID(0, 0)), "lorem ipsum")))
                .when(eventRepository).loadOrderByVersionASC(TodoId.from(new UUID(0, 0)));

        // When
        final Todo todoMarkedAsDone = todoCommandHandler.handle(givenMarkTodoAsDone);

        // Then
        assertAll(
                () -> assertThat(todoMarkedAsDone.id()).isEqualTo(TodoId.from(new UUID(0, 0))),
                () -> assertThat(todoMarkedAsDone.description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoMarkedAsDone.status()).isEqualTo(Status.DONE),
                () -> assertThat(todoMarkedAsDone.important()).isEqualTo(Boolean.FALSE),
                () -> verify(eventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                new AggregateVersion(1),
                                new TodoMarkedAsDone(TodoId.from(new UUID(0, 0)))))
                )
        );
    }
}
