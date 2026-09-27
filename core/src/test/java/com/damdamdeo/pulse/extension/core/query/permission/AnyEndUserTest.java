package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

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
        when(permissionExecutionContext.isEndUser()).thenReturn(true);

        // When
        final boolean allowed = AnyEndUser.INSTANCE.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(permissionExecutionContext).isEndUser());
    }

    @Test
    void shouldDisallowWhenNotAnEndUser() throws QueryException {
        // Given
        when(permissionExecutionContext.isEndUser()).thenReturn(false);

        // When
        final boolean allowed = AnyEndUser.INSTANCE.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(permissionExecutionContext).isEndUser());
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given

        // When
        final int priority = AnyEndUser.INSTANCE.priority();

        // Then
        assertEquals(1, priority);
    }
}
