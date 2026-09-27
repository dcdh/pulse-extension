package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.permission.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.Input;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisibilityRoleRestrictedTest {

    @Mock
    Input mock;

    @Mock
    Result<Projection> result;

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    @Test
    void shouldAllowWhenExecutedByHasOneOfVisibilityRoles() throws QueryException {
        // Given
        final List<String> visibilityRoles = List.of("ADMIN", "USER");
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(visibilityRoles);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(true);

        // When
        final boolean allowed = VisibilityRoleRestricted.INSTANCE.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER")
        );
    }

    @Test
    void shouldAllowWhenExecutedByHasFirstVisibilityRole() throws QueryException {
        // Given
        final List<String> visibilityRoles = List.of("ADMIN", "USER");
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(visibilityRoles);
        when(executionContext.hasRole("ADMIN")).thenReturn(true);

        // When
        final boolean allowed = VisibilityRoleRestricted.INSTANCE.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext, never()).hasRole("USER")
        );
    }

    @Test
    void shouldDisallowWhenExecutedByHasNoneOfVisibilityRoles() throws QueryException {
        // Given
        final List<String> visibilityRoles = List.of("ADMIN", "USER");

        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(visibilityRoles);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(false);

        // When
        final boolean allowed = VisibilityRoleRestricted.INSTANCE.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER")
        );
    }

    @Test
    void shouldDisallowWhenVisibilityRolesAreEmpty() throws QueryException {
        // Given
        when(permissionExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(permissionExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of());

        // When
        final boolean allowed = VisibilityRoleRestricted.INSTANCE.allow(Set.of(), permissionExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verifyNoInteractions(executionContext)
        );
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given

        // When
        final int priority = VisibilityRoleRestricted.INSTANCE.priority();

        // Then
        assertEquals(2, priority);
    }
}
