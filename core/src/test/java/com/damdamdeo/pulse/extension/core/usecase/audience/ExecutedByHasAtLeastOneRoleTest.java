package com.damdamdeo.pulse.extension.core.usecase.audience;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutedByHasAtLeastOneRoleTest {

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    AudienceExecutionContext audienceExecutionContext;

    @Test
    void shouldAllowWhenExecutedByHasOneOfTheRequiredRoles() throws UseCaseException {
        // Given
        final ExecutedByHasAtLeastOneRole audience = new ExecutedByHasAtLeastOneRole("ADMIN", "USER");
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(true);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER")
        );
    }

    @Test
    void shouldAllowWhenExecutedByHasFirstRequiredRole() throws UseCaseException {
        // Given
        final ExecutedByHasAtLeastOneRole audience = new ExecutedByHasAtLeastOneRole("ADMIN", "USER");
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.hasRole("ADMIN")).thenReturn(true);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertTrue(allowed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext, never()).hasRole("USER")
        );
    }

    @Test
    void shouldReturnFalseWhenExecutedByHasNoneOfTheRequiredRoles() throws UseCaseException {
        // Given
        final ExecutedByHasAtLeastOneRole audience = new ExecutedByHasAtLeastOneRole("ADMIN", "USER");
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(false);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER")
        );
    }

    @Test
    void shouldReturnFalseWhenRequiredRolesAreEmpty() throws UseCaseException {
        // Given
        final ExecutedByHasAtLeastOneRole audience = new ExecutedByHasAtLeastOneRole();
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);

        // When
        final boolean allowed = audience.allow(TodoId.USER_1_TODO_1, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertFalse(allowed),
                () -> verifyNoInteractions(executionContext)
        );
    }

    @Test
    void shouldHaveExpectedPriority() {
        // Given
        final ExecutedByHasAtLeastOneRole audience =
                new ExecutedByHasAtLeastOneRole("ADMIN");

        // When
        final int priority = audience.priority();

        // Then
        assertEquals(4, priority);
    }
}
