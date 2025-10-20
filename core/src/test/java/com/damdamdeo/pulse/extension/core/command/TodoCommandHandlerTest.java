package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoCommandHandlerTest {

    TodoCommandHandler todoCommandHandler;

    @Mock
    EventRepository<Todo, TodoId> eventRepository;

    @BeforeEach
    void setUp() {
        todoCommandHandler = new TodoCommandHandler(new JvmCommandHandlerRegistry(), eventRepository, new StubTransaction());
    }

    @Test
    void shouldCreateTodo() {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo(new TodoId("Damien", 0L), "lorem ipsum");
        doReturn(List.of()).when(eventRepository).loadOrderByVersionASC(new TodoId("Damien", 0L));

        // When
        final Todo todoCreated = todoCommandHandler.handle(givenCreateTodo);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(new TodoId("Damien", 0L)),
                () -> assertThat(todoCreated.description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoCreated.status()).isEqualTo(Status.IN_PROGRESS),
                () -> assertThat(todoCreated.important()).isEqualTo(Boolean.FALSE),
                () -> verify(eventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                new AggregateVersion(0),
                                new NewTodoCreated(new TodoId("Damien", 0L), "lorem ipsum"))),
                        todoCreated
                )
        );
    }

    @Test
    void shouldClassifieAsImportant() {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo(new TodoId("Damien", 0L), "IMPORTANT lorem ipsum");
        doReturn(List.of()).when(eventRepository).loadOrderByVersionASC(new TodoId("Damien", 0L));

        // When
        final Todo todoCreated = todoCommandHandler.handle(givenCreateTodo);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(new TodoId("Damien", 0L)),
                () -> assertThat(todoCreated.description()).isEqualTo("IMPORTANT lorem ipsum"),
                () -> assertThat(todoCreated.status()).isEqualTo(Status.IN_PROGRESS),
                () -> assertThat(todoCreated.important()).isEqualTo(Boolean.TRUE),
                () -> verify(eventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                        new AggregateVersion(0),
                                        new NewTodoCreated(new TodoId("Damien", 0L), "IMPORTANT lorem ipsum")),
                                new VersionizedEvent<>(
                                        new AggregateVersion(1),
                                        new ClassifiedAsImportant(new TodoId("Damien", 0L)))),
                        todoCreated)
        );
    }

    @Test
    void shouldMarkTodoAsDone() {
        // Given
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(new TodoId("Damien", 0L));
        doReturn(List.of(new NewTodoCreated(new TodoId("Damien", 0L), "lorem ipsum")))
                .when(eventRepository).loadOrderByVersionASC(new TodoId("Damien", 0L));

        // When
        final Todo todoMarkedAsDone = todoCommandHandler.handle(givenMarkTodoAsDone);

        // Then
        assertAll(
                () -> assertThat(todoMarkedAsDone.id()).isEqualTo(new TodoId("Damien", 0L)),
                () -> assertThat(todoMarkedAsDone.description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoMarkedAsDone.status()).isEqualTo(Status.DONE),
                () -> assertThat(todoMarkedAsDone.important()).isEqualTo(Boolean.FALSE),
                () -> verify(eventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                new AggregateVersion(1),
                                new TodoMarkedAsDone(new TodoId("Damien", 0L)))),
                        todoMarkedAsDone
                )
        );
    }
}
