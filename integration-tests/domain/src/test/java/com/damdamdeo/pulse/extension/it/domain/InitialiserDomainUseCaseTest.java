package com.damdamdeo.pulse.extension.it.domain;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.AddNewTodoItem;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.command.RegisterUser;
import com.damdamdeo.pulse.extension.core.connecteduser.registration.UserRegistrationDomainUseCase;
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
class InitialiserDomainUseCaseTest {

    @Mock
    UserRegistrationDomainUseCase userRegistrationDomainUseCase;

    @Mock
    CommandHandler<Todo, TodoId> todoCommandHandler;

    @Mock
    CommandHandler<TodoChecklist, TodoChecklistId> todoChecklistCommandHandler;

    InitialiserUseCase initialiserUseCase;

    @BeforeEach
    void setup() {
        initialiserUseCase = new InitialiserUseCase(userRegistrationDomainUseCase, todoCommandHandler, todoChecklistCommandHandler);
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
        initialiserUseCase.execute(command);

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

    @Test
    void shouldThrowNullPointerExceptionWhenCommandIsNull() {
        // When / Then
        assertThatThrownBy(() -> initialiserUseCase.execute(null))
                .isInstanceOf(NullPointerException.class);
    }
}
