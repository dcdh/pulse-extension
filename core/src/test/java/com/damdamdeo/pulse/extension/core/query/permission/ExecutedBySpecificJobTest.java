package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.job.JobName;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.QueryException;
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
    void shouldAllowWhenExecutedByIsSpecificJobName() throws QueryException {
        // Given
        final ExecutedBySpecificJob<TodoId> permission = new ExecutedBySpecificJob<>(new JobName("SampleJob"));
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(new ExecutedBy.Job(new JobName("SampleJob")));

        // When
        final boolean allowed = permission.allow(ProjectionId.from(TodoId.USER_1_TODO_1), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(permissionExecutionContext).executionContextProvider(),
                () -> verify(executionContext).executedBy()
        );
    }

    @Test
    void shouldDisallowWhenExecutedByIsSpecificJobName() throws QueryException {
        // Given
        final ExecutedBySpecificJob<TodoId> permission = new ExecutedBySpecificJob<>(new JobName("SampleJob"));
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(new ExecutedBy.Job(new JobName("OtherJob")));

        // When
        final boolean allowed = permission.allow(ProjectionId.from(TodoId.USER_1_TODO_1), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(permissionExecutionContext).executionContextProvider(),
                () -> verify(executionContext).executedBy()
        );
    }
}
