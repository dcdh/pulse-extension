package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.*;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.NotAvailableExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.saga.OnStoredEventListener;
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

    List<OnStoredEventListener<TodoId, Event<TodoId>>> todoOnStoredEventListeners = new ArrayList<>();

    List<OnStoredEventListener<TodoChecklistId, Event<TodoChecklistId>>> todoChecklistOnStoredEventListeners = new ArrayList<>();

    Function<SequenceNumber, TodoId> creational = sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber);

    @BeforeEach
    void setUp() {
        todoCommandHandler = new TodoCommandHandler(new JvmCommandHandlerRegistry(), todoEventRepository, new StubTransaction(),
                notAvailableExecutedByProvider, todoOnStoredEventListeners, aggregateIdGenerator);
        todoChecklistCommandHandler = new TodoChecklistCommandHandler(new JvmCommandHandlerRegistry(), todoChecklistEventRepository, new StubTransaction(),
                notAvailableExecutedByProvider, todoChecklistOnStoredEventListeners, aggregateIdGenerator);
    }

    @Test
    void shouldCreateTodoUsingExecutedByProvider() throws CommandException, SequenceGenerationException {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo("lorem ipsum");
        doReturn(false).when(todoEventRepository).hasEventsFor(TodoId.USER_1_TODO_1);
        doReturn(TodoId.USER_1_TODO_1).when(aggregateIdGenerator).generate(TodoId.class, creational);

        // When
        final Todo todoCreated = todoCommandHandler.handle(creational, givenCreateTodo, DuplicateTodoException::new);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(TodoId.USER_1_TODO_1),
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
    void shouldCreateTodoChecklistUsingTodoOwning() throws SequenceGenerationException, CommandException {
        // Given
        final Function<SequenceNumber, TodoChecklistId> creational = sequenceNumber -> new TodoChecklistId(TodoId.USER_1_TODO_1, sequenceNumber);
        final BelongsTo belongsTo = TodoChecklist.BELONGS_TO_USER_1_TODO_1;
        final AddNewTodoItem givenCreateTodoChecklist = new AddNewTodoItem(TodoId.USER_1_TODO_1, "lorem ipsum");
        doReturn(false).when(todoChecklistEventRepository).hasEventsFor(TodoChecklistId.USER_1_TODO_1_1);
        doReturn(TodoChecklistId.USER_1_TODO_1_1)
                .when(aggregateIdGenerator).generate(new For<>(TodoChecklistId.class, belongsTo), creational);

        // When
        final TodoChecklist todoChecklistCreated = todoChecklistCommandHandler.handle(creational, givenCreateTodoChecklist, DuplicateTodoChecklistException::new);

        // Then
        assertAll(
                () -> assertThat(todoChecklistCreated.id()).isEqualTo(TodoChecklistId.USER_1_TODO_1_1),
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
    void shouldClassifieAsImportant() throws CommandException, SequenceGenerationException {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo("IMPORTANT lorem ipsum");
        doReturn(false).when(todoEventRepository).hasEventsFor(TodoId.USER_1_TODO_1);
        doReturn(TodoId.USER_1_TODO_1).when(aggregateIdGenerator).generate(TodoId.class, creational);

        // When
        final Todo todoCreated = todoCommandHandler.handle(creational, givenCreateTodo, DuplicateTodoException::new);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(TodoId.USER_1_TODO_1),
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
    void shouldMarkTodoAsDone() throws CommandException {
        // Given
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(TodoId.USER_1_TODO_1);
        doReturn(List.of(new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)))
                .when(todoEventRepository).loadOrderByVersionASC(TodoId.USER_1_TODO_1);

        // When
        final Todo todoMarkedAsDone = todoCommandHandler.handle(givenMarkTodoAsDone, () -> new UnknownTodoException(TodoId.USER_1_TODO_1));

        // Then
        assertAll(
                () -> assertThat(todoMarkedAsDone.id()).isEqualTo(TodoId.USER_1_TODO_1),
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
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(TodoId.USER_1_TODO_1);
        doReturn(List.of(
                new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE),
                new ExecutedByEvent<>(new TodoMarkedAsDone(), ExecutedBy.NotAvailable.INSTANCE)))
                .when(todoEventRepository).loadOrderByVersionASC(TodoId.USER_1_TODO_1);

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(givenMarkTodoAsDone, () -> new UnknownTodoException(TodoId.USER_1_TODO_1)))
                .isExactlyInstanceOf(CommandException.class)
                .cause()
                .isExactlyInstanceOf(BusinessException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("la todo U000001-T000001 doit être in progress");
    }

    @Test
    void shouldThrowCommandException() {
        // Given
        final FailTodo failTodo = new FailTodo(TodoId.USER_1_TODO_1);
        doReturn(List.of(new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)))
                .when(todoEventRepository).loadOrderByVersionASC(TodoId.USER_1_TODO_1);

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(failTodo, () -> new UnknownTodoException(TodoId.USER_1_TODO_1)))
                .isExactlyInstanceOf(CommandException.class)
                .cause()
                .isExactlyInstanceOf(BusinessException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Fail !");
    }

    @Test
    void shouldFailWhenCommandIsNotHandled() {
        // Given
        final UnhandledTodo unhandledTodo = new UnhandledTodo(TodoId.USER_1_TODO_1);
        doReturn(List.of(new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)))
                .when(todoEventRepository).loadOrderByVersionASC(TodoId.USER_1_TODO_1);

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(unhandledTodo, () -> new UnknownTodoException(TodoId.USER_1_TODO_1)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Missing 'handle' method for command class - you must implement the method 'public void handle(final UnhandledTodo unhandledTodo, final ExecutionContext executionContext, final EventAppender eventAppender) throws BusinessException' in 'Todo'");
    }

    @Test
    void shouldFailWhenEventIsNotHandled() {
        // Given
        final CommandWithoutOnEvent commandWithoutOnEvent = new CommandWithoutOnEvent(TodoId.USER_1_TODO_1);
        doReturn(List.of(new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)))
                .when(todoEventRepository).loadOrderByVersionASC(TodoId.USER_1_TODO_1);

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(commandWithoutOnEvent, () -> new UnknownTodoException(TodoId.USER_1_TODO_1)))
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
    void shouldThrowCommandExceptionHavingTodoMissingExceptionCauseWhenMissing() {
        // Given
        final MarkTodoAsDone givenMarkTodoAsDone = new MarkTodoAsDone(TodoId.USER_1_TODO_1);
        doReturn(List.of()).when(todoEventRepository).loadOrderByVersionASC(TodoId.USER_1_TODO_1);

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(givenMarkTodoAsDone, () -> new UnknownTodoException(TodoId.USER_1_TODO_1)))
                .isExactlyInstanceOf(CommandException.class)
                .cause()
                .isExactlyInstanceOf(UnknownTodoException.class)
                .hasFieldOrPropertyWithValue("todoId", TodoId.USER_1_TODO_1);
    }

    @Test
    void shouldThrowCommandExceptionHavingDuplicateTodoExceptionWhenDuplicate() throws SequenceGenerationException {
        // Given
        doReturn(TodoId.USER_1_TODO_1).when(aggregateIdGenerator).generate(TodoId.class, creational);
        final CreateTodo givenCreateTodo = new CreateTodo("IMPORTANT lorem ipsum");
        doReturn(true).when(todoEventRepository).hasEventsFor(TodoId.USER_1_TODO_1);

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(creational, givenCreateTodo, DuplicateTodoException::new))
                .isExactlyInstanceOf(CommandException.class)
                .cause()
                .isExactlyInstanceOf(DuplicateTodoException.class)
                .hasFieldOrPropertyWithValue("todoId", TodoId.USER_1_TODO_1);
    }

    @Test
    void shouldFailFastWhenCreationalCommandCallByCommandHandler() {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo("IMPORTANT lorem ipsum");

        // When && Then
        assertThatThrownBy(() -> todoCommandHandler.handle(givenCreateTodo, () -> new UnknownTodoException(TodoId.USER_1_TODO_1)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("You must use handle(final K id, final CreationalCommand<K> creationalCommand, final Supplier<DuplicateAggregateException> duplicateAggregateExceptionSupplier)");
    }
}
