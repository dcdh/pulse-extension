package com.damdamdeo.pulse.extension.it.domain;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.AddNewTodoItem;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.command.RegisterUser;
import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedUserAggregateIdProvider;
import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedUserAggregateIdProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialiserUseCaseTest {

    @Mock
    private ConnectedUserAggregateIdProvider connectedUserAggregateIdProvider;

    @Mock
    private CommandHandler<User, UserId> userCommandHandler;

    @Mock
    private CommandHandler<Todo, TodoId> todoCommandHandler;

    @Mock
    private CommandHandler<TodoChecklist, TodoChecklistId> todoChecklistCommandHandler;

    private InitialiserUseCase initialiserUseCase;

    @BeforeEach
    void setUp() {
        initialiserUseCase = new InitialiserUseCase(
                connectedUserAggregateIdProvider,
                userCommandHandler,
                todoCommandHandler,
                todoChecklistCommandHandler);
    }

    @Test
    void shouldExecuteInitialisationSuccessfully() throws Exception {
        // Given
        final InitialiserCommand command = new InitialiserCommand();

        final UserId userId = UserId.USER_1;

        final User user = new User(userId);

        final TodoId todoId = new TodoId(userId, TodoId.SEQUENCE_NUMBER_1);
        final Todo todo = new Todo(todoId);

        final TodoChecklistId todoChecklistId = new TodoChecklistId(todoId, TodoChecklistId.SEQUENCE_NUMBER_1);
        final TodoChecklist todoChecklist = new TodoChecklist(todoChecklistId);

        when(connectedUserAggregateIdProvider.provide(eq(UserId.class), any(), any())).thenReturn(userId);
        when(userCommandHandler.handle(eq(userId), any(RegisterUser.class), any())).thenReturn(user);
        when(todoCommandHandler.handle(
                ArgumentMatchers.<Function<SequenceNumber, TodoId>>any(),
                eq(new CreateTodo("lorem ipsum")), any())).thenReturn(todo);
        when(todoChecklistCommandHandler.handle(
                ArgumentMatchers.<Function<SequenceNumber, TodoChecklistId>>any(),
                eq(new AddNewTodoItem(todoId, "Make it works !")),
                any()
        )).thenReturn(todoChecklist);

        // When
        initialiserUseCase.execute(command);

        // Then
        assertAll(
                () -> verify(connectedUserAggregateIdProvider, times(1)).provide(eq(UserId.class), any(), any()),
                () -> verify(userCommandHandler, times(1)).handle(any(UserId.class), any(RegisterUser.class), any()),
                () -> verify(todoCommandHandler).handle(ArgumentMatchers.<Function<SequenceNumber, TodoId>>any(), any(CreateTodo.class), any()),
                () -> verify(todoChecklistCommandHandler).handle(ArgumentMatchers.<Function<SequenceNumber, TodoChecklistId>>any(),
                        any(AddNewTodoItem.class), any()));
    }

    @Test
    void shouldThrowTechnicalExceptionWhenConnectedUserProviderFails() throws Exception {
        // Given
        final InitialiserCommand command = new InitialiserCommand();

        when(connectedUserAggregateIdProvider.provide(
                eq(UserId.class),
                any(),
                any()
        )).thenThrow(new ConnectedUserAggregateIdProviderException(new RuntimeException("Something went wrong")));

        // When / Then
        assertThatThrownBy(() -> initialiserUseCase.execute(command))
                .isInstanceOf(TechnicalException.class)
                .cause()
                .isInstanceOf(ConnectedUserAggregateIdProviderException.class)
                .cause()
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Something went wrong");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenCommandIsNull() {
        // When / Then
        assertThatThrownBy(() -> initialiserUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldPropagateBusinessException() throws Exception {
        // Given
        final InitialiserCommand command = new InitialiserCommand();

        final UserId userId = UserId.USER_1;

        when(connectedUserAggregateIdProvider.provide(eq(UserId.class), any(), any())).thenReturn(userId);
        when(userCommandHandler.handle(eq(userId), any(RegisterUser.class), any()))
                .thenThrow(new DuplicateUserException(userId));

        // When / Then
        assertThatThrownBy(() -> initialiserUseCase.execute(command))
                .isInstanceOf(DuplicateUserException.class)
                .hasFieldOrPropertyWithValue("userId", UserId.USER_1);
    }

    @Test
    void shouldPropagateTodoDuplicateException() throws Exception {
        // Given
        final InitialiserCommand command = new InitialiserCommand();

        final UserId userId = UserId.USER_1;
        final User user = new User(userId);

        when(connectedUserAggregateIdProvider.provide(eq(UserId.class), any(), any())).thenReturn(userId);
        when(userCommandHandler.handle(eq(userId), any(RegisterUser.class), any())).thenReturn(user);
        when(todoCommandHandler.handle(
                ArgumentMatchers.<Function<SequenceNumber, TodoId>>any(),
                any(CreateTodo.class), any())).thenThrow(new DuplicateTodoException(TodoId.USER_1_TODO_1));

        // When / Then
        assertThatThrownBy(() -> initialiserUseCase.execute(command))
                .isInstanceOf(DuplicateTodoException.class)
                .hasFieldOrPropertyWithValue("todoId", TodoId.USER_1_TODO_1);
    }

    @Test
    void shouldPropagateTodoChecklistDuplicateException() throws Exception {
        // Given
        final InitialiserCommand command = new InitialiserCommand();

        final UserId userId = UserId.USER_1;
        final User user = new User(userId);
        final TodoId todoId = TodoId.USER_1_TODO_1;
        final Todo todo = new Todo(todoId);

        when(connectedUserAggregateIdProvider.provide(eq(UserId.class), any(), any())).thenReturn(userId);
        when(userCommandHandler.handle(eq(userId), any(RegisterUser.class), any())).thenReturn(user);
        when(todoCommandHandler.handle(ArgumentMatchers.<Function<SequenceNumber, TodoId>>any(),
                any(CreateTodo.class), any())).thenReturn(todo);
        when(todoChecklistCommandHandler.handle(
                ArgumentMatchers.<Function<SequenceNumber, TodoChecklistId>>any(),
                any(AddNewTodoItem.class),
                any())).thenThrow(new DuplicateTodoChecklistException(TodoChecklistId.USER_1_TODO_1_1));

        // When / Then
        assertThatThrownBy(() -> initialiserUseCase.execute(command))
                .isInstanceOf(DuplicateTodoChecklistException.class)
                .hasFieldOrPropertyWithValue("todoChecklistId", TodoChecklistId.USER_1_TODO_1_1);
    }
}
