package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EveryoneTest {

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldAllowWhenExecute() throws QueryException {
        // Given
        final Everyone<TodoId> permission = new Everyone<>();

        // When
        final boolean allowed = permission.allow(ProjectionId.from(TodoId.USER_1_TODO_1), permissionExecutionContext);

        // Then
        assertTrue(allowed);
    }
}
