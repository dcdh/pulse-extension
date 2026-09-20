package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.TodoId;
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
class AnyEndUserTest {

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldAllowWhenEndUserExecute() throws UseCaseException {
        // Given
        when(permissionExecutionContext.isEndUser()).thenReturn(true);

        // When
        final boolean allowed = AnyEndUser.INSTANCE.allow(TodoId.USER_1_TODO_1, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(permissionExecutionContext).isEndUser());
    }

    @Test
    void shouldReturnFalseWhenNotAnEndUser() throws UseCaseException {
        // Given
        when(permissionExecutionContext.isEndUser()).thenReturn(false);

        // When
        final boolean allowed = AnyEndUser.INSTANCE.allow(TodoId.USER_1_TODO_1, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(permissionExecutionContext).isEndUser());
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given
        final AnyEndUser permission = AnyEndUser.INSTANCE;

        // When
        final int priority = permission.priority();

        // Then
        assertEquals(1, priority);
    }
}
