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

    TodoChecklistCommandHandler todoChecklistCommandHandler;

    @Mock
    EventRepository<Todo, TodoId> todoEventRepository;

    @Mock
    EventRepository<TodoChecklist, TodoChecklistId> todoChecklistEventRepository;

    @Spy
    NotAvailableExecutionContextProvider notAvailableExecutedByProvider;

    @Mock
    AggregateIdGenerator aggregateIdGenerator;

    List<Saga<TodoId, Event<TodoId>>> todoSagas = new ArrayList<>();

    List<Saga<TodoChecklistId, Event<TodoChecklistId>>> todoChecklistSagas = new ArrayList<>();

    Function<SequenceNumber, TodoId> creational = sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber);

    // TodoId.SEQUENCE_NUMBER_1)
    @BeforeEach
    void setUp() throws SequenceGenerationException {
        todoCommandHandler = new TodoCommandHandler(new JvmCommandHandlerRegistry(), todoEventRepository, new StubTransaction(),
                notAvailableExecutedByProvider, todoSagas, aggregateIdGenerator);
        todoChecklistCommandHandler = new TodoChecklistCommandHandler(new JvmCommandHandlerRegistry(), todoChecklistEventRepository, new StubTransaction(),
                notAvailableExecutedByProvider, todoChecklistSagas, aggregateIdGenerator);
    }

    @Test
    void shouldCreateTodoUsingExecutedByProvider() throws BusinessException, SequenceGenerationException {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo("lorem ipsum");
        doReturn(false).when(todoEventRepository).hasEventsFor(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));
        doReturn(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1)).when(aggregateIdGenerator).generate(TodoId.class, creational);

        // When
        final Todo todoCreated = todoCommandHandler.handle(creational, givenCreateTodo, DuplicateTodoException::new);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1)),
                () -> assertThat(todoCreated.description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoCreated.status()).isEqualTo(Status.IN_PROGRESS),
                () -> assertThat(todoCreated.important()).isEqualTo(Boolean.FALSE),
                () -> verify(todoEventRepository, times(1)).save(
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
    void shouldCreateTodoChecklistUsingTodoOwning() throws SequenceGenerationException, BusinessException {
        // Given
        final Function<SequenceNumber, TodoChecklistId> creational = sequenceNumber -> new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), sequenceNumber);
        final BelongsTo belongsTo = User.BELONGS_TO_USER_1_TODO_1;
        final AddNewTodoItem givenCreateTodoChecklist = new AddNewTodoItem(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), "lorem ipsum");
        doReturn(false).when(todoChecklistEventRepository).hasEventsFor(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1));
        doReturn(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1))
                .when(aggregateIdGenerator).generate(new For<>(TodoChecklistId.class, belongsTo), creational);

        // When
        final TodoChecklist todoChecklistCreated = todoChecklistCommandHandler.handle(creational, givenCreateTodoChecklist, DuplicateTodoChecklistException::new);

        // Then
        assertAll(
                () -> assertThat(todoChecklistCreated.id()).isEqualTo(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1)),
                () -> assertThat(todoChecklistCreated.description()).isEqualTo("lorem ipsum"),
                () -> verify(todoChecklistEventRepository, times(1)).save(
                        List.of(new VersionizedEvent<>(
                                new AggregateVersion(0),
                                new ExecutedByEvent<>(new TodoItemAdded("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE))),
                        todoChecklistCreated,
                        ExecutedBy.NotAvailable.INSTANCE
                ),
                () -> verify(notAvailableExecutedByProvider, times(2)).provide()
        );
    }

    @Test
    void shouldClassifieAsImportant() throws BusinessException, SequenceGenerationException {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo("IMPORTANT lorem ipsum");
        doReturn(false).when(todoEventRepository).hasEventsFor(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));
        doReturn(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1)).when(aggregateIdGenerator).generate(TodoId.class, creational);

        // When
        final Todo todoCreated = todoCommandHandler.handle(creational, givenCreateTodo, DuplicateTodoException::new);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1)),
                () -> assertThat(todoCreated.description()).isEqualTo("IMPORTANT lorem ipsum"),
                () -> assertThat(todoCreated.status()).isEqualTo(Status.IN_PROGRESS),
                () -> assertThat(todoCreated.important()).isEqualTo(Boolean.TRUE),
                () -> verify(todoEventRepository, times(1)).save(
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
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));
        doReturn(List.of(new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)))
                .when(todoEventRepository).loadOrderByVersionASC(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));

        // When
        final Todo todoMarkedAsDone = todoCommandHandler.handle(givenMarkTodoAsDone);

        // Then
        assertAll(
                () -> assertThat(todoMarkedAsDone.id()).isEqualTo(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1)),
                () -> assertThat(todoMarkedAsDone.description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoMarkedAsDone.status()).isEqualTo(Status.DONE),
                () -> assertThat(todoMarkedAsDone.important()).isEqualTo(Boolean.FALSE),
                () -> verify(todoEventRepository, times(1)).save(
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
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));
        doReturn(List.of(
                new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE),
                new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE)))
                .when(todoEventRepository).loadOrderByVersionASC(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(givenMarkTodoAsDone))
                .isInstanceOf(BusinessException.class)
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("la todo U000001-T000001 doit être in progress");
    }

    @Test
    void shouldThrowBusinessException() {
        // Given
        final FailTodo failTodo = new FailTodo(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(failTodo))
                .isInstanceOf(BusinessException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Fail !");
    }

    @Test
    void shouldFailWhenCommandIsNotHandled() {
        // Given
        final UnhandledTodo unhandledTodo = new UnhandledTodo(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(unhandledTodo))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Missing 'handle' method for command class - you must implement the method 'public void handle(final UnhandledTodo unhandledTodo, final ExecutionContext executionContext, final EventAppender eventAppender) throws BusinessException' in 'Todo'");
    }

    @Test
    void shouldFailWhenEventIsNotHandled() {
        // Given
        final CommandWithoutOnEvent commandWithoutOnEvent = new CommandWithoutOnEvent(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));

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

    static final class DuplicateTodoChecklistException extends DuplicateAggregateException {

        private final TodoChecklistId todoChecklistId;

        public DuplicateTodoChecklistException(final TodoChecklistId todoChecklistId) {
            this.todoChecklistId = Objects.requireNonNull(todoChecklistId);
        }
    }

    @Test
    void shouldThrowBusinessExceptionHavingTodoMissingExceptionCauseWhenMissing() {
        // Given
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));
        doReturn(List.of()).when(todoEventRepository).loadOrderByVersionASC(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(givenMarkTodoAsDone, () -> new UnknownTodoException(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1))))
                .isInstanceOf(BusinessException.class)
                .rootCause()
                .isInstanceOf(MissingAggregateException.class)
                .hasFieldOrPropertyWithValue("todoId", new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));
    }

    @Test
    void shouldThrowBusinessExceptionHavingDuplicateTodoExceptionWhenDuplicate() throws SequenceGenerationException {
        // Given
        doReturn(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1)).when(aggregateIdGenerator).generate(TodoId.class, creational);
        final CreateTodo givenCreateTodo = new CreateTodo("IMPORTANT lorem ipsum");
        doReturn(true).when(todoEventRepository).hasEventsFor(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(creational, givenCreateTodo, DuplicateTodoException::new))
                .isInstanceOf(BusinessException.class)
                .rootCause()
                .isInstanceOf(DuplicateTodoException.class)
                .hasFieldOrPropertyWithValue("todoId", new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1));
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
