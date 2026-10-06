package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.permission.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.permission.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.query.permission.Everyone;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;
import com.damdamdeo.pulse.extension.core.query.permission.VisibilityRoleRestricted;
import com.damdamdeo.pulse.extension.core.traceability.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardQueryUseCaseTest {

    private static final Input INPUT = new SampleInput();

    @Mock
    SingleResult<TodoId, Projection<TodoId>> result;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;

    @Mock
    ExecutedByResolver executedByResolver;

    @Mock
    QueryUseCase<TodoId, Input, Projection<TodoId>, SingleResult<TodoId, Projection<TodoId>>> decorated;

    @Mock
    TraceAppender traceAppender;

    private GuardQueryUseCase<TodoId, Input, Projection<TodoId>, SingleResult<TodoId, Projection<TodoId>>> guardQuery;

    @BeforeEach
    void setUp() {
        guardQuery = new GuardQueryUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, decorated, traceAppender) {
        };
    }

    @Test
    void shouldReturnResultWhenEveryoneAllowsAccess() throws QueryException {
        // Given
        when(decorated.permissions()).thenReturn(List.of(new Everyone<>()));
        when(decorated.execute(INPUT)).thenReturn(result);

        // When
        final SingleResult<TodoId, Projection<TodoId>> executed = guardQuery.execute(INPUT);

        // Then
        assertAll(
                () -> assertSame(result, executed),
                () -> verify(decorated).execute(INPUT),
                () -> verify(traceAppender).append(INPUT, result.aggregateIds(), ExecutionStatus.SUCCESS)
        );
    }

    @Test
    void shouldReturnResultFromFirstAudienceThatAllowsAccess() throws QueryException {
        // Given
        when(decorated.execute(INPUT)).thenReturn(result);
        final VisibilityRoleRestricted<TodoId> visibilityRoleRestricted = new VisibilityRoleRestricted<>();
        when(decorated.permissions()).thenReturn(List.of(visibilityRoleRestricted, new Everyone<>()));
        when(decorated.execute(INPUT)).thenReturn(result);

        // When
        final Result<TodoId, Projection<TodoId>> executed = guardQuery.execute(INPUT);

        // Then
        assertAll(
                () -> assertSame(result, executed),
                () -> verify(decorated).execute(INPUT),
                () -> verify(traceAppender).append(INPUT, result.aggregateIds(), ExecutionStatus.SUCCESS),
                () -> verify(decorated).execute(any())
        );
    }

    @Test
    void shouldExecutePermissionsInPriorityOrder() throws QueryException {
        // Given
        when(decorated.execute(INPUT)).thenReturn(result);
        final VisibilityRoleRestricted<TodoId> visibilityRoleRestricted = new VisibilityRoleRestricted<>();
        when(decorated.permissions()).thenReturn(List.of(new Everyone<>(), visibilityRoleRestricted));
        when(decorated.execute(INPUT)).thenReturn(result);

        // When
        final SingleResult<TodoId, Projection<TodoId>> executed = guardQuery.execute(INPUT);

        // Then
        assertAll(
                () -> assertSame(result, executed),
                () -> verify(decorated).execute(INPUT),
                () -> verify(traceAppender).append(INPUT, result.aggregateIds(), ExecutionStatus.SUCCESS),
                () -> verify(decorated).execute(any())
        );
    }

    @Test
    void shouldNotExecuteFollowingPermissionsWhenEveryoneAllowsAccess() throws QueryException {
        // Given
        when(decorated.permissions()).thenReturn(List.of(new Everyone<>(), new VisibilityRoleRestricted<>()));
        when(decorated.execute(INPUT)).thenReturn(result);

        // When
        final SingleResult<TodoId, Projection<TodoId>> executed = guardQuery.execute(INPUT);

        // Then
        assertAll(
                () -> assertSame(result, executed),
                () -> verify(decorated).execute(INPUT),
                () -> verifyNoInteractions(executionContextProvider, backendUserVisibilityRolesProvider,
                        executedByResolver)
        );
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenNoAudienceAllowsAccess() throws QueryException {
        // Given
        when(decorated.execute(INPUT)).thenReturn(result);
        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.ServiceAccount("backend"), Set.of("reader"));
        when(decorated.permissions()).thenReturn(List.of(new VisibilityRoleRestricted<>()));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("admin"));

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> guardQuery.execute(INPUT))
                        .isExactlyInstanceOf(QueryException.class)
                        .cause()
                        .isExactlyInstanceOf(UnauthorizedException.class),
                () -> verify(decorated).permissions(),
                () -> verify(traceAppender).append(
                        INPUT,
                        result.aggregateIds(),
                        ExecutionStatus.FAILED_UNAUTHORIZED
                ),
                () -> verify(decorated).execute(any())
        );
    }

    @Test
    void shouldAppendTraceWhenAccessIsGranted() throws QueryException, TraceAppenderException {
        // Given
        when(decorated.permissions()).thenReturn(List.of(new Everyone<>()));
        when(decorated.execute(INPUT)).thenReturn(result);

        // When
        guardQuery.execute(INPUT);

        // Then
        verify(traceAppender).append(INPUT, result.aggregateIds(), ExecutionStatus.SUCCESS);
    }

    @Test
    void shouldThrowInfrastructureFailureWhenTraceAppendingFails() throws QueryException, TraceAppenderException {
        // Given
        final Set<TodoId> aggregateIds = Set.of();
        when(decorated.permissions()).thenReturn(List.of(new Everyone<>()));
        when(decorated.execute(INPUT)).thenReturn(result);
        when(result.aggregateIds()).thenReturn(aggregateIds);
        doThrow(new TraceAppenderException(new RuntimeException("Something wrong happened")))
                .when(traceAppender).append(INPUT, aggregateIds, ExecutionStatus.SUCCESS);

        // When / Then
        final QueryException exception = assertThrows(QueryException.class, () -> guardQuery.execute(INPUT));

        assertAll(
                () -> assertEquals(QueryExceptionCode.INFRASTRUCTURE_FAILURE, exception.queryExceptionCode()),
                () -> verify(decorated).execute(INPUT),
                () -> verify(traceAppender).append(any(Input.class), any(), any())
        );
    }

    @Test
    void shouldDelegatePermissions() {
        // Given
        final List<Permission<TodoId>> permissions = List.of(new Everyone<>(), new VisibilityRoleRestricted<>());
        when(decorated.permissions()).thenReturn(permissions);

        // When
        final List<Permission<TodoId>> result = guardQuery.permissions();

        // Then
        assertSame(permissions, result);
    }
}
