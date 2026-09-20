package com.damdamdeo.pulse.extension.core.usecase.permission;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.permission.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisibilityRoleRestrictedTest {

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldAllowWhenExecutedByHasOneOfVisibilityRoles() throws UseCaseException {
        // Given
        final List<String> visibilityRoles = List.of("ADMIN", "USER");
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(visibilityRoles);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(true);

        // When
        final boolean allowed = VisibilityRoleRestricted.INSTANCE.allow(TodoId.USER_1_TODO_1, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER")
        );
    }

    @Test
    void shouldAllowWhenExecutedByHasFirstVisibilityRole() throws UseCaseException {
        // Given
        final List<String> visibilityRoles = List.of("ADMIN", "USER");
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(visibilityRoles);
        when(executionContext.hasRole("ADMIN")).thenReturn(true);

        // When
        final boolean allowed = VisibilityRoleRestricted.INSTANCE.allow(TodoId.USER_1_TODO_1, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext, never()).hasRole("USER")
        );
    }

    @Test
    void shouldReturnFalseWhenExecutedByHasNoneOfVisibilityRoles() throws UseCaseException {
        // Given
        final List<String> visibilityRoles = List.of("ADMIN", "USER");
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(visibilityRoles);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(false);

        // When
        final boolean allowed = VisibilityRoleRestricted.INSTANCE.allow(TodoId.USER_1_TODO_1, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER")
        );
    }

    @Test
    void shouldReturnFalseWhenVisibilityRolesAreEmpty() throws UseCaseException {
        // Given
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of());

        // When
        final boolean allowed = VisibilityRoleRestricted.INSTANCE.allow(TodoId.USER_1_TODO_1, permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verifyNoInteractions(executionContext)
        );
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given

        // When
        final int priority = VisibilityRoleRestricted.INSTANCE.priority();

        // Then
        assertEquals(1, priority);
    }
}
