package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.audience.Audience;
import com.damdamdeo.pulse.extension.core.query.audience.Everyone;
import com.damdamdeo.pulse.extension.core.query.audience.VisibilityRoleRestricted;
import com.damdamdeo.pulse.extension.core.traceability.From;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppender;
import com.damdamdeo.pulse.extension.core.traceability.TraceAppenderException;
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
    Result<Projection> result;

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;

    @Mock
    ExecutedByResolver executedByResolver;

    @Mock
    QueryUseCase<Input, Projection> decorated;

    @Mock
    TraceAppender traceAppender;

    private GuardQueryUseCase<Input, Projection> guardQuery;

    @BeforeEach
    void setUp() {
        guardQuery = new GuardQueryUseCase<>(executionContextProvider, backendUserVisibilityRolesProvider,
                executedByResolver, decorated, traceAppender) {
        };
    }

    @Test
    void shouldReturnResultWhenEveryoneAllowsAccess() throws QueryException {
        // Given
        when(decorated.audiences()).thenReturn(List.of(Everyone.INSTANCE));
        when(decorated.execute(INPUT)).thenReturn(result);

        // When
        final Result<Projection> executed = guardQuery.execute(INPUT);

        // Then
        assertAll(
                () -> assertSame(result, executed),
                () -> verify(decorated).execute(INPUT),
                () -> verify(traceAppender).append(eq(result), any(From.class))
        );
    }

    @Test
    void shouldReturnResultFromFirstAudienceThatAllowsAccess() throws QueryException {
        // Given
        final VisibilityRoleRestricted visibilityRoleRestricted = VisibilityRoleRestricted.INSTANCE;
        when(decorated.audiences()).thenReturn(List.of(visibilityRoleRestricted, Everyone.INSTANCE));
        when(decorated.execute(INPUT)).thenReturn(result);

        // When
        final Result<Projection> executed = guardQuery.execute(INPUT);

        // Then
        assertAll(
                () -> assertSame(result, executed),
                () -> verify(decorated).execute(INPUT),
                () -> verify(traceAppender).append(eq(result), any(From.class))
        );
    }

    @Test
    void shouldExecuteAudiencesInPriorityOrder() throws QueryException {
        // Given
        final VisibilityRoleRestricted visibilityRoleRestricted = VisibilityRoleRestricted.INSTANCE;
        when(decorated.audiences()).thenReturn(List.of(Everyone.INSTANCE, visibilityRoleRestricted));
        when(decorated.execute(INPUT)).thenReturn(result);

        // When
        final Result<Projection> executed = guardQuery.execute(INPUT);

        // Then
        assertAll(
                () -> assertSame(result, executed),
                () -> verify(decorated).execute(INPUT),
                () -> verify(traceAppender).append(eq(result), any(From.class))
        );
    }

    @Test
    void shouldNotExecuteFollowingAudiencesWhenEveryoneAllowsAccess() throws QueryException {
        // Given
        when(decorated.audiences()).thenReturn(List.of(Everyone.INSTANCE, VisibilityRoleRestricted.INSTANCE));
        when(decorated.execute(INPUT)).thenReturn(result);

        // When
        final Result<Projection> executed = guardQuery.execute(INPUT);

        // Then
        assertAll(
                () -> assertSame(result, executed),
                () -> verify(decorated).execute(INPUT),
                () -> verifyNoInteractions(executionContextProvider, backendUserVisibilityRolesProvider,
                        executedByResolver)
        );
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenNoAudienceAllowsAccess() {
        // Given
        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.ServiceAccount("backend"), Set.of("reader"));
        when(decorated.audiences()).thenReturn(List.of(VisibilityRoleRestricted.INSTANCE));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("admin"));

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> guardQuery.execute(INPUT))
                        .isExactlyInstanceOf(QueryException.class)
                        .cause()
                        .isExactlyInstanceOf(UnauthorizedException.class),
                () -> verify(decorated).audiences(),
                () -> verifyNoInteractions(traceAppender)
        );
    }

    @Test
    void shouldAppendTraceWhenAccessIsGranted() throws QueryException, TraceAppenderException {
        // Given
        when(decorated.audiences()).thenReturn(List.of(Everyone.INSTANCE));
        when(decorated.execute(INPUT)).thenReturn(result);

        // When
        guardQuery.execute(INPUT);

        // Then
        verify(traceAppender).append(eq(result), any(From.class));
    }

    @Test
    void shouldThrowInfrastructureFailureWhenTraceAppendingFails() throws QueryException, TraceAppenderException {
        // Given
        when(decorated.audiences()).thenReturn(List.of(Everyone.INSTANCE));
        when(decorated.execute(INPUT)).thenReturn(result);
        doThrow(new TraceAppenderException(new RuntimeException("Something wrong happened")))
                .when(traceAppender).append(eq(result), any(From.class));

        // When / Then
        final QueryException exception = assertThrows(
                QueryException.class,
                () -> guardQuery.execute(INPUT));

        assertAll(
                () -> assertEquals(QueryExceptionCode.INFRASTRUCTURE_FAILURE, exception.queryExceptionCode()),
                () -> verify(decorated).execute(INPUT),
                () -> verify(traceAppender).append(eq(result), any(From.class))
        );
    }

    @Test
    void shouldDelegateAudiences() {
        // Given
        final List<Audience> audiences = List.of(Everyone.INSTANCE, VisibilityRoleRestricted.INSTANCE);
        when(decorated.audiences()).thenReturn(audiences);

        // When
        final List<Audience> result = guardQuery.audiences();

        // Then
        assertSame(audiences, result);
    }
}
