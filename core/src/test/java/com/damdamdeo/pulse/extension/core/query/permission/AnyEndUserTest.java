package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnyEndUserTest {

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldAllowWhenEndUserExecute() throws QueryException {
        // Given
        final AnyEndUser<TodoId> permission = new AnyEndUser<>();
        when(permissionExecutionContext.isEndUser()).thenReturn(true);

        // When
        final boolean allowed = permission.allow(TodoId.USER_1_TODO_1, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(permissionExecutionContext).isEndUser());
    }

    @Test
    void shouldDisallowWhenNotAnEndUser() throws QueryException {
        // Given
        final AnyEndUser<TodoId> permission = new AnyEndUser<>();
        when(permissionExecutionContext.isEndUser()).thenReturn(false);

        // When
        final boolean allowed = permission.allow(TodoId.USER_1_TODO_1, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(permissionExecutionContext).isEndUser());
    }
}
