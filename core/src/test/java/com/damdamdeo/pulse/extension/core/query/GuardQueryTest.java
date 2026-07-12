package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardQueryTest {

    private static final ExecutedBy.EndUser BOB = new ExecutedBy.EndUser("bob", true);

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;

    @Mock
    ExecutedByResolver executedByResolver;

    @Mock
    Query<String, TestProjection> decorated;

    GuardQuery<String, TestProjection> guardQuery;

    @BeforeEach
    void setUp() {
        guardQuery = new GuardQuery<>(
                executionContextProvider,
                backendUserVisibilityRolesProvider,
                executedByResolver,
                decorated) {
        };
    }

    @Test
    void shouldReturnDecoratedResultWhenAudienceIsEveryone() throws Exception {
        // Given
        final Result<TestProjection> expected = Result.of(TestProjection.PROJECTION_USER_1, Set.of());

        when(decorated.audiences()).thenReturn(List.of(Audience.EVERYONE));
        when(decorated.execute("input")).thenReturn(expected);

        // When
        final Result<TestProjection> actual = guardQuery.execute("input");

        // Then
        assertAll(
                () -> Assertions.assertSame(expected, actual),
                () -> verify(decorated).execute("input"),
                () -> verifyNoInteractions(
                        executionContextProvider,
                        backendUserVisibilityRolesProvider,
                        executedByResolver
                )
        );
    }

    @Test
    void shouldReturnDecoratedResultWhenAudienceIsRoleRestrictedAndUserHasRequiredRole() throws Exception {
        // Given
        final Result<TestProjection> expected = Result.of(TestProjection.PROJECTION_USER_1, Set.of());
        final ExecutionContext context = new ExecutionContext(BOB, Set.of("ADMIN"));

        when(decorated.audiences()).thenReturn(List.of(Audience.ROLE_RESTRICTED));
        when(executionContextProvider.provide()).thenReturn(context);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("ADMIN"));
        when(decorated.execute("input")).thenReturn(expected);

        // When
        final Result<TestProjection> actual = guardQuery.execute("input");

        // Then
        assertAll(
                () -> Assertions.assertSame(expected, actual),
                () -> verify(decorated).execute(any()),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide()
        );
    }

    @Test
    void shouldThrowQueryExceptionWhenAudienceIsRoleRestrictedAndUserDoesNotHaveRequiredRole() {
        // Given
        final ExecutionContext context = new ExecutionContext(BOB, Set.of("USER"));

        when(decorated.audiences()).thenReturn(List.of(Audience.ROLE_RESTRICTED));
        when(executionContextProvider.provide()).thenReturn(context);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("ADMIN"));

        // When
        final QueryException exception = Assertions.assertThrows(
                QueryException.class,
                () -> guardQuery.execute("input")
        );

        // Then
        assertAll(
                () -> Assertions.assertInstanceOf(
                        DisallowException.class,
                        exception.getCause()
                ),
                () -> verify(decorated, never()).execute(any())
        );
    }

    @Test
    void shouldReturnDecoratedResultWhenAudienceIsInExecutedByAndUserIsEligible() throws Exception {
        // Given
        final Result<TestProjection> expected = Result.of(TestProjection.PROJECTION_USER_1, Set.of());
        final ExecutionContext context = new ExecutionContext(BOB, Set.of());

        when(decorated.audiences()).thenReturn(List.of(Audience.IN_EXECUTED_BY));
        when(decorated.execute("input")).thenReturn(expected);
        when(executedByResolver.resolve(expected.aggregateIds())).thenReturn(Set.of(BOB));
        when(executionContextProvider.provide()).thenReturn(context);

        // When
        final Result<TestProjection> actual = guardQuery.execute("input");

        // Then
        assertAll(
                () -> Assertions.assertSame(expected, actual),
                () -> verify(decorated).execute(any()),
                () -> verify(executedByResolver).resolve(any()),
                () -> verify(executionContextProvider).provide()
        );
    }

    @Test
    void shouldThrowQueryExceptionWhenAudienceIsInExecutedByAndUserIsNotEligible() throws Exception {
        // Given
        final Result<TestProjection> expected = Result.of(TestProjection.PROJECTION_USER_1, Set.of());
        final ExecutionContext context = new ExecutionContext(BOB, Set.of());

        when(decorated.audiences()).thenReturn(List.of(Audience.IN_EXECUTED_BY));
        when(decorated.execute("input")).thenReturn(expected);
        when(executedByResolver.resolve(expected.aggregateIds())).thenReturn(Set.of());
        when(executionContextProvider.provide()).thenReturn(context);

        // When
        final QueryException exception = Assertions.assertThrows(
                QueryException.class,
                () -> guardQuery.execute("input")
        );

        // Then
        assertAll(
                () -> Assertions.assertInstanceOf(
                        DisallowException.class,
                        exception.getCause()
                ),
                () -> verify(decorated).execute(any()),
                () -> verify(executedByResolver).resolve(any())
        );
    }

    @Test
    void shouldReturnResultWhenFirstAudienceFailsAndSecondAudienceSucceeds() throws Exception {
        // Given
        final Result<TestProjection> expected = Result.of(TestProjection.PROJECTION_USER_1, Set.of());
        final ExecutionContext context = new ExecutionContext(BOB, Set.of("ADMIN"));

        when(decorated.audiences()).thenReturn(List.of(Audience.ROLE_RESTRICTED, Audience.EVERYONE));

        when(executionContextProvider.provide()).thenReturn(context);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("SUPER_ADMIN"));
        when(decorated.execute("input")).thenReturn(expected);

        // When
        final Result<TestProjection> actual = guardQuery.execute("input");

        // Then
        assertAll(
                () -> Assertions.assertSame(expected, actual),
                () -> verify(decorated, times(1)).execute(any())
        );
    }

    @Test
    void shouldThrowQueryExceptionWhenNoAudienceAllowsExecution() throws QueryException {
        // Given
        final ExecutionContext context = new ExecutionContext(BOB, Set.of());
        final Result<TestProjection> expected = Result.of(TestProjection.PROJECTION_USER_1, Set.of());

        when(decorated.audiences()).thenReturn(List.of(Audience.ROLE_RESTRICTED,
                Audience.IN_EXECUTED_BY));

        when(executionContextProvider.provide()).thenReturn(context);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("ADMIN"));
        when(decorated.execute("input")).thenReturn(expected);
        when(executedByResolver.resolve(expected.aggregateIds())).thenReturn(Set.of());

        // When
        final QueryException exception = Assertions.assertThrows(
                QueryException.class,
                () -> guardQuery.execute("input")
        );

        // Then
        assertAll(
                () -> Assertions.assertInstanceOf(
                        DisallowException.class,
                        exception.getCause()
                ),
                () -> verify(decorated).execute(any()),
                () -> verify(executedByResolver).resolve(any())
        );
    }
}
