package com.damdamdeo.pulse.extension.it.domain;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.*;
import com.damdamdeo.pulse.extension.core.connecteduser.registration.UserRegistrationDomainUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreationalWorkflowTest {

    @Mock
    UserRegistrationDomainUseCase userRegistrationDomainUseCase;

    @Mock
    CommandHandler<Todo, TodoId> todoCommandHandler;

    @Mock
    CommandHandler<TodoChecklist, TodoChecklistId> todoChecklistCommandHandler;

    CreationalWorkflow creationalWorkflow;

    @BeforeEach
    void setup() {
        creationalWorkflow = new CreationalWorkflow(userRegistrationDomainUseCase, todoCommandHandler, todoChecklistCommandHandler);
    }

    @Test
    void shouldExecuteInitialisationSuccessfully() throws Exception {
        // Given
        final InitialiserCommand command = new InitialiserCommand();

        final UserId userId = UserId.USER_1;

        final Handled<User, UserId> user = new Handled<>(new User(userId), List.of());

        final TodoId todoId = new TodoId(userId, TodoId.SEQUENCE_NUMBER_1);
        final Handled<Todo, TodoId> todo = new Handled<>(new Todo(todoId), List.of());

        final TodoChecklistId todoChecklistId = new TodoChecklistId(todoId, TodoChecklistId.SEQUENCE_NUMBER_1);
        final Handled<TodoChecklist, TodoChecklistId> todoChecklist = new Handled<>(
                new TodoChecklist(todoChecklistId), List.of());

        when(userRegistrationDomainUseCase.execute(new RegisterUser())).thenReturn(user);
        when(todoCommandHandler.handle(
                ArgumentMatchers.<Function<SequenceNumber, TodoId>>any(),
                eq(new CreateTodo("lorem ipsum")),
                ArgumentMatchers.<Function<TodoId, DuplicateAggregateException>>any())).thenReturn(todo);
        when(todoChecklistCommandHandler.handle(
                ArgumentMatchers.<Function<SequenceNumber, TodoChecklistId>>any(),
                eq(new AddNewTodoItem(todoId, "Make it works !")),
                ArgumentMatchers.<Function<TodoChecklistId, DuplicateAggregateException>>any()
        )).thenReturn(todoChecklist);

        // When
        creationalWorkflow.execute(command);

        // Then
        assertAll(
                () -> verify(userRegistrationDomainUseCase, times(1)).execute(any()),
                () -> verify(todoCommandHandler).handle(ArgumentMatchers.<Function<SequenceNumber, TodoId>>any(),
                        any(CreateTodo.class),
                        ArgumentMatchers.<Function<TodoId, DuplicateAggregateException>>any()),
                () -> verify(todoChecklistCommandHandler).handle(ArgumentMatchers.<Function<SequenceNumber, TodoChecklistId>>any(),
                        any(AddNewTodoItem.class),
                        ArgumentMatchers.<Function<TodoChecklistId, DuplicateAggregateException>>any()));
    }
}
