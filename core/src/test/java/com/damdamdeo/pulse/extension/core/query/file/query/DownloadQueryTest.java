package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.encryption.Decrypted;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionException;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.query.file.*;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.FiligraneApplier;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.UnableToApplyFiligraneException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DownloadQueryTest {

    private final FileRepository fileRepository = mock(FileRepository.class);
    private final ExecutionContextProvider executionContextProvider = mock(ExecutionContextProvider.class);
    private final ExecutedByResolver executedByResolver = mock(ExecutedByResolver.class);
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider = mock(BackendUserVisibilityRolesProvider.class);
    private final DecryptionService decryptionService = mock(DecryptionService.class);
    private final FiligraneApplier filigraneApplier = mock(FiligraneApplier.class);

    private DownloadQuery query;

    @BeforeEach
    void setUp() {
        query = new DownloadQuery(fileRepository, executionContextProvider, executedByResolver,
                backendUserVisibilityRolesProvider, decryptionService, filigraneApplier);
    }

    @Test
    void shouldDownloadWithFiligraneWhenBackendUserHasVisibilityRole() throws Exception {
        // Given
        final DownloadInput input = downloadInput();
        final FileInfo fileInfo = fileInfo();
        final FileContent fileContent = fileContent();
        final FileContent decryptedContent = decryptedFileContent();
        final FileContent filigranedContent = filigranedFileContent();

        final ExecutionContext executionContext = backendUserExecutionContext();
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(input.fileIdentifier())).thenReturn(fileInfo);
        when(executedByResolver.resolve(fileInfo.ownedBy())).thenReturn(Set.of(new ExecutedBy.EndUser("BOB", true)));
        when(fileRepository.getFileContentByFileIdentifier(input.fileIdentifier())).thenReturn(fileContent);
        when(decryptionService.decrypt(any(Encrypted.class), eq(fileInfo.ownedBy()), any())).thenReturn(new Decrypted<>(decryptedContent));
        when(filigraneApplier.apply(decryptedContent)).thenReturn(filigranedContent);

        // When
        final FileContent result = query.execute(input);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(filigranedContent),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verify(fileRepository).getFileInfoByFileIdentifier(input.fileIdentifier()),
                () -> verify(executedByResolver).resolve(fileInfo.ownedBy()),
                () -> verify(fileRepository).getFileContentByFileIdentifier(input.fileIdentifier()),
                () -> verify(decryptionService).decrypt(any(Encrypted.class), eq(fileInfo.ownedBy()), any()),
                () -> verify(filigraneApplier).apply(decryptedContent)
        );
    }

    @Test
    void shouldDownloadWithoutFiligraneWhenExecutedByIsEligible() throws Exception {
        // Given
        final DownloadInput input = downloadInput();
        final FileInfo fileInfo = fileInfo();
        final FileContent fileContent = fileContent();
        final FileContent decryptedContent = decryptedFileContent();
        final ExecutedBy executedBy = new ExecutedBy.EndUser("BOB", true);
        final ExecutionContext executionContext = new ExecutionContext(executedBy, Set.of("user"));

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(input.fileIdentifier())).thenReturn(fileInfo);
        when(executedByResolver.resolve(fileInfo.ownedBy())).thenReturn(Set.of(executedBy));
        when(fileRepository.getFileContentByFileIdentifier(input.fileIdentifier())).thenReturn(fileContent);
        when(decryptionService.decrypt(any(Encrypted.class), eq(fileInfo.ownedBy()), any())).thenReturn(new Decrypted<>(decryptedContent));

        // When
        final FileContent result = query.execute(input);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(decryptedContent),
                () -> verify(decryptionService).decrypt(any(Encrypted.class), eq(fileInfo.ownedBy()), any()),
                () -> verify(filigraneApplier, never()).apply(any())
        );
    }

    @Test
    void shouldDownloadWithoutFiligraneWhenExecutionContextHasVisibilityRoleAndEligibleUserIsDifferent() throws Exception {
        // Given
        final DownloadInput input = downloadInput();
        final FileInfo fileInfo = fileInfo();
        final FileContent fileContent = fileContent();
        final FileContent decryptedContent = decryptedFileContent();
        final FileContent filigranedContent = filigranedFileContent();

        final ExecutionContext executionContext = backendUserExecutionContext();
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(input.fileIdentifier())).thenReturn(fileInfo);
        when(executedByResolver.resolve(fileInfo.ownedBy())).thenReturn(Set.of(new ExecutedBy.EndUser("ALICE", true)));
        when(fileRepository.getFileContentByFileIdentifier(input.fileIdentifier())).thenReturn(fileContent);
        when(decryptionService.decrypt(any(Encrypted.class), eq(fileInfo.ownedBy()), any())).thenReturn(new Decrypted<>(decryptedContent));
        when(filigraneApplier.apply(decryptedContent)).thenReturn(filigranedContent);

        // When
        final FileContent result = query.execute(input);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(filigranedContent),
                () -> verify(filigraneApplier).apply(decryptedContent)
        );
    }

    @Test
    void shouldRejectWhenUserHasNoVisibilityRoleAndIsNotEligible() throws Exception {
        // Given
        final DownloadInput input = downloadInput();
        final FileInfo fileInfo = fileInfo();

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser("ALICE", true),
                Set.of("user"));
        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(input.fileIdentifier())).thenReturn(fileInfo);
        when(executedByResolver.resolve(fileInfo.ownedBy())).thenReturn(Set.of(new ExecutedBy.EndUser("BOB", true)));

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(input))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.FORBIDDEN)
                        .hasCauseInstanceOf(UnauthorizedException.class),
                () -> verify(fileRepository).getFileInfoByFileIdentifier(input.fileIdentifier()),
                () -> verify(executedByResolver).resolve(fileInfo.ownedBy()),
                () -> verify(fileRepository, never()).getFileContentByFileIdentifier(any()),
                () -> verifyNoInteractions(decryptionService, filigraneApplier)
        );
    }

    @Test
    void shouldWrapFileRepositoryExceptionWhenGettingFileInfo() throws Exception {
        // Given
        final DownloadInput input = downloadInput();
        final FileRepositoryException exception = new FileRepositoryException(new IllegalStateException("Database unavailable"));
        when(executionContextProvider.provide()).thenReturn(backendUserExecutionContext());
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(input.fileIdentifier())).thenThrow(exception);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(input))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isSameAs(exception),
                () -> verify(fileRepository).getFileInfoByFileIdentifier(input.fileIdentifier()),
                () -> verifyNoInteractions(executedByResolver, decryptionService, filigraneApplier)
        );
    }

    @Test
    void shouldWrapFileRepositoryExceptionWhenGettingFileContent() throws Exception {
        // Given
        final DownloadInput input = downloadInput();
        final FileInfo fileInfo = fileInfo();
        final FileRepositoryException exception = new FileRepositoryException(new IllegalStateException("Database unavailable"));
        when(executionContextProvider.provide()).thenReturn(backendUserExecutionContext());
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(input.fileIdentifier())).thenReturn(fileInfo);
        when(executedByResolver.resolve(fileInfo.ownedBy())).thenReturn(Set.of());
        when(fileRepository.getFileContentByFileIdentifier(input.fileIdentifier())).thenThrow(exception);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(input))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isSameAs(exception),
                () -> verify(fileRepository).getFileContentByFileIdentifier(input.fileIdentifier()),
                () -> verifyNoInteractions(decryptionService, filigraneApplier)
        );
    }

    @Test
    void shouldWrapUnableToResolveException() throws Exception {
        // Given
        final DownloadInput input = downloadInput();
        final FileInfo fileInfo = fileInfo();
        final UnableToResolveException exception = new UnableToResolveException(new IllegalStateException("Unable to resolve executed by"));
        when(executionContextProvider.provide()).thenReturn(backendUserExecutionContext());
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(input.fileIdentifier())).thenReturn(fileInfo);
        when(executedByResolver.resolve(fileInfo.ownedBy())).thenThrow(exception);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(input))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isSameAs(exception),
                () -> verify(executedByResolver).resolve(fileInfo.ownedBy()),
                () -> verify(fileRepository, never()).getFileContentByFileIdentifier(any())
        );
    }

    @Test
    void shouldWrapDecryptionException() throws Exception {
        // Given
        final DownloadInput input = downloadInput();
        final FileInfo fileInfo = fileInfo();
        final FileContent fileContent = fileContent();
        final DecryptionException exception = new DecryptionException(new IllegalStateException("Unable to decrypt"));
        when(executionContextProvider.provide()).thenReturn(backendUserExecutionContext());
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(input.fileIdentifier())).thenReturn(fileInfo);
        when(executedByResolver.resolve(fileInfo.ownedBy())).thenReturn(Set.of());
        when(fileRepository.getFileContentByFileIdentifier(input.fileIdentifier())).thenReturn(fileContent);
        when(decryptionService.decrypt(any(Encrypted.class), eq(fileInfo.ownedBy()), any())).thenThrow(exception);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(input))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isSameAs(exception),
                () -> verify(decryptionService).decrypt(any(Encrypted.class), eq(fileInfo.ownedBy()), any()),
                () -> verifyNoInteractions(filigraneApplier)
        );
    }

    @Test
    void shouldWrapUnableToApplyFiligraneException() throws Exception {
        // Given
        final DownloadInput input = downloadInput();
        final FileInfo fileInfo = fileInfo();
        final FileContent fileContent = fileContent();
        final FileContent decryptedContent = decryptedFileContent();

        final UnableToApplyFiligraneException exception = new UnableToApplyFiligraneException(new IllegalStateException("Unable to apply filigrane"));
        when(executionContextProvider.provide()).thenReturn(backendUserExecutionContext());
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(input.fileIdentifier())).thenReturn(fileInfo);
        when(executedByResolver.resolve(fileInfo.ownedBy())).thenReturn(Set.of());
        when(fileRepository.getFileContentByFileIdentifier(input.fileIdentifier())).thenReturn(fileContent);
        when(decryptionService.decrypt(any(Encrypted.class), eq(fileInfo.ownedBy()), any())).thenReturn(
                new Decrypted<>(decryptedContent));
        when(filigraneApplier.apply(decryptedContent)).thenThrow(exception);

        // When / Then
        assertThatThrownBy(() -> query.execute(input))
                .isInstanceOf(QueryException.class)
                .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                .cause()
                .isSameAs(exception);
    }

    @Test
    void shouldRejectNullDownloadInput() {
        // Given
        // Nothing

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(null))
                        .isInstanceOf(NullPointerException.class),
                () -> verifyNoInteractions(fileRepository, executionContextProvider, executedByResolver,
                        backendUserVisibilityRolesProvider, decryptionService, filigraneApplier)
        );
    }

    private DownloadInput downloadInput() {
        return new DownloadInput(
                new FileIdentifier("file-123")
        );
    }

    private FileInfo fileInfo() {
        final FileIdentifier identifier = new FileIdentifier("file-123");

        return new FileInfo(
                identifier,
                new Filename("facture.jpg"),
                ContentType.IMAGE_JPG,
                new ContentLength(287759L),
                uploadedAt(),
                new UploadedBy(new ExecutedBy.EndUser("BOB", true)),
                OwnedBy.from(identifier),
                fileMetadata()
        );
    }

    private FileContent fileContent() {
        return new FileContent(
                new FileIdentifier("file-123"),
                ContentType.IMAGE_JPG,
                new ContentLength(287759L),
                new ByteArrayInputStream("encrypted-content".getBytes())
        );
    }

    private FileContent decryptedFileContent() {
        return new FileContent(
                new FileIdentifier("file-123"),
                ContentType.IMAGE_JPG,
                new ContentLength(287759L),
                new ByteArrayInputStream("decrypted-content".getBytes())
        );
    }

    private FileContent filigranedFileContent() {
        return new FileContent(
                new FileIdentifier("file-123"),
                ContentType.IMAGE_JPG,
                new ContentLength(287759L),
                new ByteArrayInputStream("filigraned-content".getBytes())
        );
    }

    private FileMetadata fileMetadata() {
        return new FileMetadata(
                Map.of(
                        "author", List.of("BOB"),
                        "tag", List.of("invoice")
                )
        );
    }

    private UploadedAt uploadedAt() {
        return new UploadedAt(
                ZonedDateTime.of(
                        LocalDate.of(2026, 8, 5),
                        LocalTime.of(23, 0, 31),
                        ZoneOffset.UTC
                )
        );
    }

    private ExecutionContext backendUserExecutionContext() {
        return new ExecutionContext(
                new ExecutedBy.EndUser("ADMIN", true),
                Set.of("backend-user")
        );
    }
}
