package com.damdamdeo.pulse.extension.core.query.audience;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.audience.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    AudienceExecutionContext audienceExecutionContext;

    @Test
    void shouldReturnDecoratedResultWhenExecutedByHasOneOfVisibilityRoles() throws QueryException {
        // Given
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        final List<String> visibilityRoles = List.of("ADMIN", "USER");
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(audienceExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(visibilityRoles);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(true);
        when(decorated.execute(mock)).thenReturn(result);

        // When
        final Optional<Result<Projection>> executed = VisibilityRoleRestricted.INSTANCE.execute(
                mock, decorated, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER"),
                () -> verify(decorated).execute(mock)
        );
    }

    @Test
    void shouldReturnDecoratedResultWhenExecutedByHasFirstVisibilityRole() throws QueryException {
        // Given
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        final List<String> visibilityRoles = List.of("ADMIN", "USER");
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(audienceExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(visibilityRoles);
        when(executionContext.hasRole("ADMIN")).thenReturn(true);
        when(decorated.execute(mock)).thenReturn(result);

        // When
        final Optional<Result<Projection>> executed = VisibilityRoleRestricted.INSTANCE.execute(
                mock, decorated, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(result), executed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext, never()).hasRole("USER"),
                () -> verify(decorated).execute(mock)
        );
    }

    @Test
    void shouldReturnEmptyWhenExecutedByHasNoneOfVisibilityRoles() throws QueryException {
        // Given
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        final List<String> visibilityRoles = List.of("ADMIN", "USER");

        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(audienceExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(visibilityRoles);
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(executionContext.hasRole("USER")).thenReturn(false);

        // When
        final Optional<Result<Projection>> executed = VisibilityRoleRestricted.INSTANCE.execute(
                mock, decorated, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.empty(), executed),
                () -> verify(executionContext).hasRole("ADMIN"),
                () -> verify(executionContext).hasRole("USER"),
                () -> verifyNoInteractions(decorated)
        );
    }

    @Test
    void shouldReturnEmptyWhenVisibilityRolesAreEmpty() throws QueryException {
        // Given
        final QueryUseCase<Input, Projection> decorated = mock(QueryUseCase.class);
        when(audienceExecutionContext.executionContextProvider()).thenReturn(executionContextProvider);
        when(audienceExecutionContext.backendUserVisibilityRolesProvider()).thenReturn(backendUserVisibilityRolesProvider);
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of());

        // When
        final Optional<Result<Projection>> executed = VisibilityRoleRestricted.INSTANCE.execute(
                mock, decorated, audienceExecutionContext);

        // Then
        assertAll(
                () -> assertEquals(Optional.empty(), executed),
                () -> verifyNoInteractions(executionContext),
                () -> verifyNoInteractions(decorated)
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
