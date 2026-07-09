package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardQueryTest {

    @Mock
    ExecutionContextProvider executionContextProvider;

    @Mock
    BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;

    @Mock
    ParticipantsProvider participantsProvider;

    @Mock
    Query<String, Projection> decorated;

    @Mock
    ExecutionContext executionContext;

    private GuardQuery<String, Projection> query;

    @BeforeEach
    void setUp() {
        query = new GuardQuery<>(executionContextProvider, backendUserVisibilityRolesProvider, participantsProvider, decorated);
    }

    @Test
    void shouldReturnProjectionWhenAudienceIsEveryone() throws Exception {
        // Given
        final Projection projection = TestProjection.PROJECTION_USER_1;

        when(decorated.audiences()).thenReturn(List.of(Audience.EVERYONE));
        when(decorated.execute("input")).thenReturn(projection);

        // When
        final Projection result = query.execute("input");

        // Then
        assertAll(
                () -> assertSame(projection, result),
                () -> verify(decorated, times(1)).execute("input"),
                () -> verifyNoInteractions(executionContextProvider),
                () -> verifyNoInteractions(backendUserVisibilityRolesProvider),
                () -> verifyNoInteractions(participantsProvider)
        );
    }

    @Test
    void shouldReturnProjectionWhenRoleRestrictedAndUserHasRole() throws Exception {
        // Given
        final Projection projection = TestProjection.PROJECTION_USER_1;

        when(decorated.audiences()).thenReturn(List.of(Audience.ROLE_RESTRICTED));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("ADMIN"));
        when(executionContext.hasRole("ADMIN")).thenReturn(true);
        when(decorated.execute("input")).thenReturn(projection);

        // When
        final Projection result = query.execute("input");

        // Then
        assertAll(
                () -> assertSame(projection, result),
                () -> verify(executionContextProvider, times(1)).provide(),
                () -> verify(backendUserVisibilityRolesProvider, times(1)).provide(),
                () -> verify(decorated, times(1)).execute("input")
        );
    }

    @Test
    void shouldThrowQueryExceptionWhenRoleRestrictedAndUserHasNoRole() {
        // Given
        when(decorated.audiences()).thenReturn(List.of(Audience.ROLE_RESTRICTED));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("ADMIN"));
        when(executionContext.hasRole("ADMIN")).thenReturn(false);

        // When
        final QueryException exception = assertThrows(
                QueryException.class,
                () -> query.execute("input")
        );

        // Then
        assertAll(
                () -> assertInstanceOf(DisallowException.class, exception.getCause()),
                () -> verify(executionContextProvider, times(1)).provide(),
                () -> verify(backendUserVisibilityRolesProvider, times(1)).provide(),
                () -> verify(decorated, never()).execute(any())
        );
    }

    @Test
    void shouldReturnProjectionWhenParticipant() throws Exception {
        // Given
        final ExecutedBy executedBy = new ExecutedBy.EndUser("alice", true);
        final Projection projection = TestProjection.PROJECTION_USER_1;

        when(decorated.audiences()).thenReturn(List.of(Audience.PARTICIPANT));
        when(decorated.execute("input")).thenReturn(projection);

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(participantsProvider.findParticipants(List.of(UserId.USER_1))).thenReturn(Set.of(executedBy));

        // When
        final Projection result = query.execute("input");

        // Then
        assertAll(
                () -> assertSame(projection, result),
                () -> verify(decorated, times(1)).execute("input"),
                () -> verify(participantsProvider, times(1)).findParticipants(any()),
                () -> verify(executionContextProvider, times(1)).provide()
        );
    }

    @Test
    void shouldThrowQueryExceptionWhenNotParticipant() throws Exception {
        // Given
        final Projection projection = TestProjection.PROJECTION_USER_1;
        final ExecutedBy executedBy = new ExecutedBy.EndUser("alice", true);
        final ExecutedBy anotherUser = new ExecutedBy.EndUser("bob", true);

        when(decorated.audiences()).thenReturn(List.of(Audience.PARTICIPANT));
        when(decorated.execute("input")).thenReturn(projection);

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(executionContext.executedBy()).thenReturn(executedBy);
        when(participantsProvider.findParticipants(List.of(UserId.USER_1))).thenReturn(Set.of(anotherUser));

        // When
        final QueryException exception = assertThrows(QueryException.class, () -> query.execute("input"));

        // Then
        assertAll(
                () -> assertInstanceOf(DisallowException.class, exception.getCause()),
                () -> verify(decorated, times(1)).execute("input"),
                () -> verify(participantsProvider, times(1)).findParticipants(List.of(UserId.USER_1)),
                () -> verify(executionContextProvider, times(1)).provide()
        );
    }

    @Test
    void shouldTryNextAudienceWhenPreviousAudienceDoesNotMatch() throws Exception {
        // Given
        final Projection projection = TestProjection.PROJECTION_USER_1;

        when(decorated.audiences()).thenReturn(List.of(Audience.ROLE_RESTRICTED, Audience.EVERYONE));

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("ADMIN"));
        when(executionContext.hasRole("ADMIN")).thenReturn(false);
        when(decorated.execute("input")).thenReturn(projection);

        // When
        final Projection result = query.execute("input");

        // Then
        assertAll(
                () -> assertSame(projection, result),
                () -> verify(executionContextProvider, times(1)).provide(),
                () -> verify(backendUserVisibilityRolesProvider, times(1)).provide(),
                () -> verify(decorated, times(1)).execute("input")
        );
    }
}
