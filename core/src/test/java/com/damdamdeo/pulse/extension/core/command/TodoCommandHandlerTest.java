package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.NotAvailableExecutionContextProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoCommandHandlerTest {

    TodoCommandHandler todoCommandHandler;

    @Mock
    EventRepository<Todo, TodoId> eventRepository;

    @Spy
    NotAvailableExecutionContextProvider notAvailableExecutedByProvider;

    @Mock
    EventNotifier eventNotifier;

    @BeforeEach
    void setUp() {
        todoCommandHandler = new TodoCommandHandler(new JvmCommandHandlerRegistry(), eventRepository, new StubTransaction(),
                notAvailableExecutedByProvider, eventNotifier);
    }

    @Test
    void shouldCreateTodoUsingExecutedByProvider() throws BusinessException {
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
                        List.of(new VersionizedEvent(
                                new AggregateVersion(0),
                                new ExecutedByEvent(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE))),
                        todoCreated,
                        ExecutedBy.NotAvailable.INSTANCE
                ),
                () -> verify(notAvailableExecutedByProvider, times(2)).provide()
        );
    }

    @Test
    void shouldClassifieAsImportant() throws BusinessException {
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
                        List.of(new VersionizedEvent(
                                        new AggregateVersion(0),
                                        new ExecutedByEvent(new NewTodoCreated("IMPORTANT lorem ipsum"),
                                                ExecutedBy.NotAvailable.INSTANCE)),
                                new VersionizedEvent(
                                        new AggregateVersion(1),
                                        new ExecutedByEvent(new ClassifiedAsImportant(),
                                                ExecutedBy.NotAvailable.INSTANCE))),
                        todoCreated,
                        ExecutedBy.NotAvailable.INSTANCE)
        );
    }

    @Test
    void shouldMarkTodoAsDone() throws BusinessException {
        // Given
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(new TodoId("Damien", 0L));
        doReturn(List.of(new ExecutedByEvent(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)))
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
                        List.of(new VersionizedEvent(
                                new AggregateVersion(1),
                                new ExecutedByEvent(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE))),
                        todoMarkedAsDone,
                        ExecutedBy.NotAvailable.INSTANCE
                )
        );
    }

    @Test
    void shouldThrowBusinessException() {
        // Given
        final FailTodo failTodo = new FailTodo(new TodoId("Damien", 0L));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(failTodo))
                .isInstanceOf(BusinessException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Fail !");
    }

    @Test
    void shouldFailWhenCommandIsNotHandled() {
        // Given
        final UnhandledTodo unhandledTodo = new UnhandledTodo(new TodoId("Damien", 0L));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(unhandledTodo))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Missing 'handle' method for command class - you must implement the method 'public void handle(final UnhandledTodo unhandledTodo, final ExecutionContext executionContext, final EventAppender eventAppender) throws BusinessException' in 'Todo'");
    }

    @Test
    void shouldFailWhenEventIsNotHandled() {
        // Given
        final CommandWithoutOnEvent commandWithoutOnEvent = new CommandWithoutOnEvent(new TodoId("Damien", 0L));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(commandWithoutOnEvent))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Missing 'on' method for event class - you must implement the method 'public void on(final Missing missing, final ExecutedBy executedBy)' in 'Todo'");
    }
}
