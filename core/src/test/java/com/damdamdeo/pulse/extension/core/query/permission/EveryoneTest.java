package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        final boolean allowed = permission.allow(Set.of(), permissionExecutionContext);

        // Then
        assertTrue(allowed);
    }

    @Test
    void shouldHaveHighestVisibilityPriority() {
        // Given
        final Everyone<TodoId> permission = new Everyone<>();

        // When
        final int priority = permission.priority();

        // Then
        assertEquals(0, priority);
    }
}
