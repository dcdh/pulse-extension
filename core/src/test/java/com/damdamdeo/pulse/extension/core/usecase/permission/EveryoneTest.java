package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class EveryoneTest {

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldReturnDecoratedResultWhenExecute() throws UseCaseException {
        // Given

        // When
        final boolean allowed = new Everyone<TodoId, MarkTodoAsDone>().allow(TodoId.USER_1_TODO_1,
                new MarkTodoAsDone(TodoId.USER_1_TODO_1), permissionExecutionContext);

        // Then
        assertThat(allowed).isTrue();
    }

    @Test
    void shouldHaveHighestVisibilityPriority() {
        // Given

        // When
        final int priority = new Everyone<TodoId, MarkTodoAsDone>().priority();

        // Then
        assertEquals(0, priority);
    }
}
