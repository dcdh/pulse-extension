package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.QueryExceptionCode;
import com.damdamdeo.pulse.extension.core.query.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.query.file.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

class GetFileInfoQueryTest {

    private final FileRepository fileRepository = mock(FileRepository.class);
    private final ExecutionContextProvider executionContextProvider = mock(ExecutionContextProvider.class);
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider =
            mock(BackendUserVisibilityRolesProvider.class);

    private GetFileInfoQuery query;

    @BeforeEach
    void setUp() {
        query = new GetFileInfoQuery(fileRepository, executionContextProvider, backendUserVisibilityRolesProvider);
    }

    @Test
    void shouldGetFileInfoWhenUserHasVisibilityRole() throws Exception {
        // Given
        final FileInfo fileInfo = fileInfo();
        final FileIdentifier fileIdentifier = fileInfo.fileIdentifier();

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser("BOB", true),
                Set.of("backend-user")
        );

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(fileIdentifier)).thenReturn(fileInfo);

        // When
        final FileInfo result = query.execute(fileIdentifier);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(fileInfo),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verify(fileRepository).getFileInfoByFileIdentifier(fileIdentifier)
        );
    }

    @Test
    void shouldGetFileInfoWhenUserHasOneOfVisibilityRoles() throws Exception {
        // Given
        final FileInfo fileInfo = fileInfo();
        final FileIdentifier fileIdentifier = fileInfo.fileIdentifier();

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser("BOB", true),
                Set.of("user", "backend-user", "admin")
        );

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide())
                .thenReturn(List.of("backend-user", "super-admin"));
        when(fileRepository.getFileInfoByFileIdentifier(fileIdentifier)).thenReturn(fileInfo);

        // When
        final FileInfo result = query.execute(fileIdentifier);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(fileInfo),
                () -> verify(fileRepository).getFileInfoByFileIdentifier(fileIdentifier)
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
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.FORBIDDEN)
                        .hasCauseInstanceOf(UnauthorizedException.class),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verifyNoInteractions(fileRepository)
        );
    }

    @Test
    void shouldWrapFileRepositoryException() throws Exception {
        // Given
        final FileInfo fileInfo = fileInfo();
        final FileIdentifier fileIdentifier = fileInfo.fileIdentifier();
        final FileRepositoryException repositoryException =
                new FileRepositoryException(new IllegalStateException("Database unavailable"));

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser("BOB", true),
                Set.of("backend-user")
        );

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(fileIdentifier))
                .thenThrow(repositoryException);

        // When / Then
        assertThatThrownBy(() -> query.execute(fileIdentifier))
                .isInstanceOf(QueryException.class)
                .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                .cause()
                .isInstanceOf(FileRepositoryException.class)
                .isSameAs(repositoryException);
    }

    private FileInfo fileInfo() {
        final FileIdentifier identifier = new FileIdentifier("file-123");
        return new FileInfo(
                identifier,
                new Filename("facture.jpg"),
                ContentType.IMAGE_JPG,
                new ContentLength(287759L),
                new UploadedAt(
                        ZonedDateTime.of(
                                LocalDate.of(2026, 8, 5),
                                LocalTime.of(23, 0, 31),
                                ZoneOffset.UTC
                        )
                ),
                new UploadedBy(new ExecutedBy.EndUser("BOB", true)),
                OwnedBy.from(identifier),
                new FileMetadata(
                        Map.of(
                                "author", List.of("BOB"),
                                "tag", List.of("invoice")
                        )
                )
        );
    }
}
