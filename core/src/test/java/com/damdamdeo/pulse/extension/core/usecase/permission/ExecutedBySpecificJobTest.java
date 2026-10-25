package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.job.JobName;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutedBySpecificJobTest {

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldAllowWhenExecutedByIsSpecificJobName() throws UseCaseException {
        // Given
        final ExecutedBySpecificJob<TodoId, MarkTodoAsDone> permission = new ExecutedBySpecificJob<>(new JobName("SampleJob"));
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(new ExecutedBy.Job(new JobName("SampleJob")));

        // When
        final boolean allowed = permission.allow(new MarkTodoAsDone(TodoId.USER_1_TODO_1), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(permissionExecutionContext).executionContextProvider(),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldDisallowWhenExecutedByIsSpecificJobName() throws UseCaseException {
        // Given
        final ExecutedBySpecificJob<TodoId, MarkTodoAsDone> permission = new ExecutedBySpecificJob<>(new JobName("SampleJob"));
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(new ExecutedBy.Job(new JobName("OtherJob")));

        // When
        final boolean allowed = permission.allow(new MarkTodoAsDone(TodoId.USER_1_TODO_1), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(permissionExecutionContext).executionContextProvider(),
                () -> verify(executionContext).executedBy()
        );
    }
}
