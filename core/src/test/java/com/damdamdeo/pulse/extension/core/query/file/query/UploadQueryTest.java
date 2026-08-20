package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionException;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionService;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.QueryExceptionCode;
import com.damdamdeo.pulse.extension.core.query.file.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

class UploadQueryTest {

    private final FileRepository fileRepository = mock(FileRepository.class);
    private final ExecutionContextProvider executionContextProvider = mock(ExecutionContextProvider.class);
    private final EncryptionService encryptionService = mock(EncryptionService.class);
    private final ImageMetadataExtractor imageMetadataExtractor = mock(ImageMetadataExtractor.class);
    private final UploadedAtProvider uploadedAtProvider = mock(UploadedAtProvider.class);

    private UploadQuery query;

    @BeforeEach
    void setUp() {
        query = new UploadQuery(fileRepository, executionContextProvider, encryptionService, imageMetadataExtractor, uploadedAtProvider);
    }

    @Test
    void shouldUploadFile() throws Exception {
        // Given
        final InputFile inputFile = inputFile();
        final FileIdentifier expected = inputFile.fileIdentifier();
        final FileMetadata metadata = fileMetadata();
        final UploadedAt uploadedAt = uploadedAt();
        final Encrypted<InputStream> encrypted = encrypted();

        when(fileRepository.exists(inputFile.fileIdentifier())).thenReturn(false);
        when(imageMetadataExtractor.extract(any(InputStream.class), eq(inputFile.contentType()))).thenReturn(metadata);
        when(encryptionService.<InputStream>encrypt(any(InputStream.class), eq(inputFile.ownedBy()), any())).thenReturn(encrypted);
        when(executionContextProvider.provide()).thenReturn(executionContext());
        when(uploadedAtProvider.provide()).thenReturn(uploadedAt);

        // When
        final FileIdentifier result = query.execute(inputFile);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(expected),
                () -> verify(fileRepository).exists(inputFile.fileIdentifier()),
                () -> verify(imageMetadataExtractor).extract(any(InputStream.class), eq(inputFile.contentType())),
                () -> verify(encryptionService).encrypt(any(InputStream.class), eq(inputFile.ownedBy()), any()),
                () -> verify(uploadedAtProvider).provide(),
                () -> verify(executionContextProvider).provide(),
                () -> verify(fileRepository).store(
                        new FileInfo(
                                inputFile.fileIdentifier(),
                                inputFile.filename(),
                                inputFile.contentType(),
                                inputFile.contentLength(),
                                uploadedAt,
                                new UploadedBy(executionContext().executedBy()),
                                inputFile.ownedBy(),
                                metadata,
                                new CustomMetadata(Map.of("key", "value"))
                        ),
                        encrypted
                )
        );
    }

    @Test
    void shouldRejectWhenFileAlreadyUploaded() throws Exception {
        // Given
        final InputFile inputFile = inputFile();
        when(fileRepository.exists(inputFile.fileIdentifier())).thenReturn(true);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(inputFile))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.CONFLICT)
                        .hasCauseInstanceOf(FileAlreadyUploadedException.class),
                () -> verify(fileRepository).exists(inputFile.fileIdentifier()),
                () -> verifyNoInteractions(executionContextProvider, encryptionService, imageMetadataExtractor, uploadedAtProvider),
                () -> verify(fileRepository, never()).store(any(), any())
        );
    }

    @Test
    void shouldRejectWhenMaximumFileSizeIsReached() throws Exception {
        // Given
        final InputFile inputFile = oversizedInputFile();

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(inputFile))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.FAIL_FAST_CONDITION_NOT_MET)
                        .hasCauseInstanceOf(MaxFileSizeReachedException.class),
                () -> verifyNoInteractions(fileRepository, executionContextProvider, encryptionService, imageMetadataExtractor, uploadedAtProvider)
        );
    }

    @Test
    void shouldWrapImageMetadataExtractorException() throws Exception {
        // Given
        final InputFile inputFile = inputFile();
        final ImageMetadataExtractorException exception =
                new ImageMetadataExtractorException(new IllegalStateException("Invalid image"));

        when(fileRepository.exists(inputFile.fileIdentifier())).thenReturn(false);
        when(imageMetadataExtractor.extract(any(InputStream.class), eq(inputFile.contentType()))).thenThrow(exception);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(inputFile))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isSameAs(exception),
                () -> verify(fileRepository).exists(inputFile.fileIdentifier()),
                () -> verify(imageMetadataExtractor).extract(any(InputStream.class), eq(inputFile.contentType())),
                () -> verifyNoInteractions(encryptionService, executionContextProvider, uploadedAtProvider),
                () -> verify(fileRepository, never()).store(any(), any())
        );
    }

    @Test
    void shouldWrapEncryptionException() throws Exception {
        // Given
        final InputFile inputFile = inputFile();
        final FileMetadata metadata = fileMetadata();
        final EncryptionException exception = new EncryptionException(new IllegalStateException("Encryption failed"));

        when(fileRepository.exists(inputFile.fileIdentifier())).thenReturn(false);
        when(imageMetadataExtractor.extract(any(InputStream.class), eq(inputFile.contentType()))).thenReturn(metadata);
        when(encryptionService.encrypt(any(InputStream.class), eq(inputFile.ownedBy()), any())).thenThrow(exception);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(inputFile))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isSameAs(exception),
                () -> verify(imageMetadataExtractor).extract(any(InputStream.class), eq(inputFile.contentType())),
                () -> verify(encryptionService).encrypt(any(InputStream.class), eq(inputFile.ownedBy()), any()),
                () -> verifyNoInteractions(executionContextProvider, uploadedAtProvider),
                () -> verify(fileRepository, never()).store(any(), any())
        );
    }

    @Test
    void shouldWrapFileRepositoryExceptionWhenCheckingIfFileExists() throws Exception {
        // Given
        final InputFile inputFile = inputFile();
        final FileRepositoryException exception = new FileRepositoryException(new IllegalStateException("Database unavailable"));
        when(fileRepository.exists(inputFile.fileIdentifier())).thenThrow(exception);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(inputFile))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isSameAs(exception),
                () -> verify(fileRepository).exists(inputFile.fileIdentifier()),
                () -> verifyNoInteractions(executionContextProvider, encryptionService, imageMetadataExtractor, uploadedAtProvider)
        );
    }

    @Test
    void shouldWrapFileRepositoryExceptionWhenStoring() throws Exception {
        // Given
        final InputFile inputFile = inputFile();
        final FileMetadata metadata = fileMetadata();
        final UploadedAt uploadedAt = uploadedAt();
        final Encrypted<InputStream> encrypted = encrypted();

        final FileRepositoryException exception = new FileRepositoryException(new IllegalStateException("Database unavailable"));
        when(fileRepository.exists(inputFile.fileIdentifier())).thenReturn(false);
        when(imageMetadataExtractor.extract(any(InputStream.class), eq(inputFile.contentType()))).thenReturn(metadata);
        when(encryptionService.<InputStream>encrypt(any(InputStream.class), eq(inputFile.ownedBy()), any())).thenReturn(encrypted);
        when(executionContextProvider.provide()).thenReturn(executionContext());
        when(uploadedAtProvider.provide()).thenReturn(uploadedAt);
        doThrow(exception).when(fileRepository).store(any(FileInfo.class), eq(encrypted));

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(inputFile))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isSameAs(exception),
                () -> verify(fileRepository).store(any(FileInfo.class), eq(encrypted))
        );
    }

    private InputFile inputFile() {
        return new InputFile(
                new FileIdentifier("file-123"),
                new ContentLength(12L),
                new ByteArrayInputStream("file-content".getBytes()),
                new Filename("facture.jpg"),
                OwnedBy.from(new FileIdentifier("owner-123")),
                new CustomMetadata(Map.of("key", "value"))
        );
    }

    private InputFile oversizedInputFile() {
        return new InputFile(
                new FileIdentifier("file-123"),
                ContentLength.ofMegaBytes(6L),
                new ByteArrayInputStream("file-content".getBytes()),
                new Filename("facture.jpg"),
                OwnedBy.from(new FileIdentifier("owner-123")),
                new CustomMetadata(Map.of("key", "value"))
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

    private ExecutionContext executionContext() {
        return new ExecutionContext(
                ExecutedBy.Anonymous.INSTANCE,
                Set.of("backend-user")
        );
    }

    @SuppressWarnings("unchecked")
    private Encrypted<InputStream> encrypted() {
        return mock(Encrypted.class);
    }
}
