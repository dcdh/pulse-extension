package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.NotAvailableExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.saga.Saga;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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
    AggregateIdGenerator aggregateIdGenerator;

    List<Saga<TodoId, Event<TodoId>>> sagas = new ArrayList<>();

    Function<SequenceNumber, TodoId> creational = sequenceNumber -> new TodoId("T", sequenceNumber);

    // TodoId.SEQUENCE_NUMBER_0)
    @BeforeEach
    void setUp() throws SequenceGenerationException {
        todoCommandHandler = new TodoCommandHandler(new JvmCommandHandlerRegistry(), eventRepository, new StubTransaction(),
                notAvailableExecutedByProvider, sagas, aggregateIdGenerator);
    }

    @Test
    void shouldCreateTodoUsingExecutedByProvider() throws BusinessException, SequenceGenerationException {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo("lorem ipsum");
        doReturn(false).when(eventRepository).hasEventsFor(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));
        doReturn(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0)).when(aggregateIdGenerator).generate(TodoId.class, creational);

        // When
        final Todo todoCreated = todoCommandHandler.handle(creational, givenCreateTodo, DuplicateTodoException::new);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0)),
                () -> assertThat(todoCreated.description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoCreated.status()).isEqualTo(Status.IN_PROGRESS),
                () -> assertThat(todoCreated.important()).isEqualTo(Boolean.FALSE),
                () -> verify(eventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                new AggregateVersion(0),
                                new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE))),
                        todoCreated,
                        ExecutedBy.NotAvailable.INSTANCE
                ),
                () -> verify(notAvailableExecutedByProvider, times(2)).provide()
        );
    }

    @Test
    void shouldClassifieAsImportant() throws BusinessException, SequenceGenerationException {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo("IMPORTANT lorem ipsum");
        doReturn(false).when(eventRepository).hasEventsFor(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));
        doReturn(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0)).when(aggregateIdGenerator).generate(TodoId.class, creational);

        // When
        final Todo todoCreated = todoCommandHandler.handle(creational, givenCreateTodo, DuplicateTodoException::new);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0)),
                () -> assertThat(todoCreated.description()).isEqualTo("IMPORTANT lorem ipsum"),
                () -> assertThat(todoCreated.status()).isEqualTo(Status.IN_PROGRESS),
                () -> assertThat(todoCreated.important()).isEqualTo(Boolean.TRUE),
                () -> verify(eventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                        new AggregateVersion(0),
                                        new ExecutedByEvent<>(new NewTodoCreated("IMPORTANT lorem ipsum"),
                                                ExecutedBy.NotAvailable.INSTANCE)),
                                new VersionizedEvent<>(
                                        new AggregateVersion(1),
                                        new ExecutedByEvent<>(new ClassifiedAsImportant(),
                                                ExecutedBy.NotAvailable.INSTANCE))),
                        todoCreated,
                        ExecutedBy.NotAvailable.INSTANCE)
        );
    }

    @Test
    void shouldMarkTodoAsDone() throws BusinessException {
        // Given
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));
        doReturn(List.of(new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)))
                .when(eventRepository).loadOrderByVersionASC(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));

        // When
        final Todo todoMarkedAsDone = todoCommandHandler.handle(givenMarkTodoAsDone);

        // Then
        assertAll(
                () -> assertThat(todoMarkedAsDone.id()).isEqualTo(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0)),
                () -> assertThat(todoMarkedAsDone.description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoMarkedAsDone.status()).isEqualTo(Status.DONE),
                () -> assertThat(todoMarkedAsDone.important()).isEqualTo(Boolean.FALSE),
                () -> verify(eventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                new AggregateVersion(1),
                                new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE))),
                        todoMarkedAsDone,
                        ExecutedBy.NotAvailable.INSTANCE
                )
        );
    }

    @Test
    void shouldFailWhenMarkingATodoDoneAlreadyDone() {
        // Given
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));
        doReturn(List.of(
                new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE),
                new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE)))
                .when(eventRepository).loadOrderByVersionASC(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(givenMarkTodoAsDone))
                .isInstanceOf(BusinessException.class)
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("la todo Damien-000000 doit être in progress");
    }

    @Test
    void shouldThrowBusinessException() {
        // Given
        final FailTodo failTodo = new FailTodo(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(failTodo))
                .isInstanceOf(BusinessException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Fail !");
    }

    @Test
    void shouldFailWhenCommandIsNotHandled() {
        // Given
        final UnhandledTodo unhandledTodo = new UnhandledTodo(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(unhandledTodo))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Missing 'handle' method for command class - you must implement the method 'public void handle(final UnhandledTodo unhandledTodo, final ExecutionContext executionContext, final EventAppender eventAppender) throws BusinessException' in 'Todo'");
    }

    @Test
    void shouldFailWhenEventIsNotHandled() {
        // Given
        final CommandWithoutOnEvent commandWithoutOnEvent = new CommandWithoutOnEvent(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(commandWithoutOnEvent))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Missing 'on' method for event class - you must implement the method 'public void on(final Missing missing, final ExecutedBy executedBy)' in 'Todo'");
    }

    static final class UnknownTodoException extends MissingAggregateException {

        private final TodoId todoId;

        public UnknownTodoException(final TodoId todoId) {
            this.todoId = Objects.requireNonNull(todoId);
        }
    }

    static final class DuplicateTodoException extends DuplicateAggregateException {

        private final TodoId todoId;

        public DuplicateTodoException(final TodoId todoId) {
            this.todoId = Objects.requireNonNull(todoId);
        }
    }

    @Test
    void shouldThrowBusinessExceptionHavingTodoMissingExceptionCauseWhenMissing() {
        // Given
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));
        doReturn(List.of()).when(eventRepository).loadOrderByVersionASC(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(givenMarkTodoAsDone, () -> new UnknownTodoException(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0))))
                .isInstanceOf(BusinessException.class)
                .rootCause()
                .isInstanceOf(MissingAggregateException.class)
                .hasFieldOrPropertyWithValue("todoId", new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));
    }

    @Test
    void shouldThrowBusinessExceptionHavingDuplicateTodoExceptionWhenDuplicate() throws SequenceGenerationException {
        // Given
        doReturn(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0)).when(aggregateIdGenerator).generate(TodoId.class, creational);
        final CreateTodo givenCreateTodo = new CreateTodo("IMPORTANT lorem ipsum");
        doReturn(true).when(eventRepository).hasEventsFor(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(creational, givenCreateTodo, DuplicateTodoException::new))
                .isInstanceOf(BusinessException.class)
                .rootCause()
                .isInstanceOf(DuplicateTodoException.class)
                .hasFieldOrPropertyWithValue("todoId", new TodoId("Damien", TodoId.SEQUENCE_NUMBER_0));
    }

    @Test
    void shouldFailFastWhenCreationalCommandCallByCommandHandler() {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo("IMPORTANT lorem ipsum");

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(givenCreateTodo))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("You must use handle(final K id, final CreationalCommand<K> creationalCommand, final Supplier<DuplicateAggregateException> duplicateAggregateExceptionSupplier)");
    }
}
