package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.traceability.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

class GetTraceByFileIdentifierQueryTest {

    private static final FileIdentifier GIVEN_FILE_IDENTIFIER = new FileIdentifier("file-123");

    private final TokenRepository tokenRepository = mock(TokenRepository.class);
    private final ExecutionContextProvider executionContextProvider = mock(ExecutionContextProvider.class);
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider =
            mock(BackendUserVisibilityRolesProvider.class);

    private GetTraceByFileIdentifierQuery query;

    @BeforeEach
    void setUp() {
        query = new GetTraceByFileIdentifierQuery(tokenRepository, executionContextProvider, backendUserVisibilityRolesProvider);
    }

    @Test
    void shouldGetTraceByFileIdentifierWhenUserHasVisibilityRole() throws Exception {
        // Given
        final List<Traceability> traceabilities = traceabilities();

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser("BOB", true),
                Set.of("backend-user")
        );

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(tokenRepository.listByFileIdentifierOrderByDownloadedAtAsc(GIVEN_FILE_IDENTIFIER)).thenReturn(traceabilities);

        // When
        final List<Traceability> result = query.execute(GIVEN_FILE_IDENTIFIER);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(traceabilities),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verify(tokenRepository).listByFileIdentifierOrderByDownloadedAtAsc(GIVEN_FILE_IDENTIFIER)
        );
    }

    @Test
    void shouldGetTraceByFileIdentifierWhenUserHasOneOfVisibilityRoles() throws Exception {
        // Given
        final List<Traceability> traceabilities = traceabilities();

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser("BOB", true),
                Set.of("user", "backend-user", "admin")
        );

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide())
                .thenReturn(List.of("backend-user", "super-admin"));
        when(tokenRepository.listByFileIdentifierOrderByDownloadedAtAsc(GIVEN_FILE_IDENTIFIER)).thenReturn(traceabilities);

        // When
        final List<Traceability> result = query.execute(GIVEN_FILE_IDENTIFIER);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(traceabilities),
                () -> verify(tokenRepository).listByFileIdentifierOrderByDownloadedAtAsc(GIVEN_FILE_IDENTIFIER)
        );
    }

    @Test
    void shouldRejectWhenUserDoesNotHaveVisibilityRole() {
        // Given
        final FileIdentifier fileIdentifier = new FileIdentifier("file-123");

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser("BOB", true),
                Set.of("user")
        );

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide())
                .thenReturn(List.of("backend-user", "admin"));

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(fileIdentifier))
                        .isInstanceOf(QueryException.class)
                        .hasCauseInstanceOf(UnauthorizedException.class),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verifyNoInteractions(tokenRepository)
        );
    }

    @Test
    void shouldWrapTokenRepositoryException() throws Exception {
        // Given
        final TokenRepositoryException repositoryException =
                new TokenRepositoryException(new IllegalStateException("Database unavailable"));

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser("BOB", true),
                Set.of("backend-user")
        );

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(tokenRepository.listByFileIdentifierOrderByDownloadedAtAsc(GIVEN_FILE_IDENTIFIER))
                .thenThrow(repositoryException);

        // When / Then
        assertThatThrownBy(() -> query.execute(GIVEN_FILE_IDENTIFIER))
                .isInstanceOf(QueryException.class)
                .cause()
                .isInstanceOf(TokenRepositoryException.class)
                .isSameAs(repositoryException);
    }

    private List<Traceability> traceabilities() {
        return List.of(
                new Traceability(new Token(new UUID(0, 0)),
                        GIVEN_FILE_IDENTIFIER, new DownloadedBy("BOB"), new DownloadedAt(ZonedDateTime.of(
                        LocalDate.of(2026, 8, 5),
                        LocalTime.of(23, 0, 31),
                        ZoneOffset.UTC
                )))
        );
    }
}
