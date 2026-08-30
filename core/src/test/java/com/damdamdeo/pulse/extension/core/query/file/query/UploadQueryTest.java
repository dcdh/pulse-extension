package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionException;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionService;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UsernameEncoder;
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

public class UploadQueryTest {

    private final FileRepository fileRepository = mock(FileRepository.class);
    private final ExecutionContextProvider executionContextProvider = mock(ExecutionContextProvider.class);
    private final EncryptionService encryptionService = mock(EncryptionService.class);
    private final ImageMetadataExtractor imageMetadataExtractor = mock(ImageMetadataExtractor.class);
    private final UploadedAtProvider uploadedAtProvider = mock(UploadedAtProvider.class);
    private final FileSizeLimitedCopier fileSizeLimitedCopier = spy(new FileSizeLimitedCopier());
    private final UsernameEncoder usernameEncoder = mock(UsernameEncoder.class);
    private final FileMetadataEncryption fileMetadataEncryption = mock(FileMetadataEncryption.class);
    private final CustomMetadataEncryption customMetadataEncryption = mock(CustomMetadataEncryption.class);

    private UploadQuery query;

    @BeforeEach
    void setUp() {
        query = new UploadQuery(fileRepository, executionContextProvider, encryptionService, imageMetadataExtractor, uploadedAtProvider,
                fileSizeLimitedCopier, usernameEncoder, fileMetadataEncryption, customMetadataEncryption);
    }

    @Test
    void shouldUploadFile() throws Exception {
        // Given
        final InputFile inputFile = inputFile();
        final FileIdentifier expected = inputFile.fileIdentifier();
        final FileMetadata fileMetadata = fileMetadata();
        final CustomMetadata customMetadata = customMetadata();
        final UploadedAt uploadedAt = uploadedAt();
        final Encrypted<InputStream> encrypted = encrypted();

        when(fileRepository.exists(inputFile.fileIdentifier())).thenReturn(false);
        when(imageMetadataExtractor.extract(any(InputStream.class), eq(inputFile.contentType()))).thenReturn(fileMetadata);
        when(encryptionService.<InputStream>encrypt(any(InputStream.class), eq(inputFile.ownedBy()), any())).thenReturn(encrypted);
        when(executionContextProvider.provide()).thenReturn(executionContext());
        when(uploadedAtProvider.provide()).thenReturn(uploadedAt);
        when(usernameEncoder.encode(new Username("bob@mail.com"), inputFile.ownedBy())).thenReturn(new UsernameEncoded("bobEncoded"));
        when(fileMetadataEncryption.encrypt(fileMetadata, inputFile.ownedBy())).thenReturn(
                new EncryptedFileMetadata(Encrypted.of("encryptedFileMetadata".getBytes()), inputFile.ownedBy()));
        when(customMetadataEncryption.encrypt(customMetadata, inputFile.ownedBy())).thenReturn(
                new EncryptedCustomMetadata(Encrypted.of("encryptedCustomMetadata".getBytes()), inputFile.ownedBy()));

        // When
        final FileIdentifier result = query.execute(inputFile);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(expected),
                () -> verify(fileRepository).exists(inputFile.fileIdentifier()),
                () -> verify(imageMetadataExtractor).extract(any(InputStream.class), any()),
                () -> verify(encryptionService).encrypt(any(InputStream.class), any(OwnedBy.class), any()),
                () -> verify(uploadedAtProvider).provide(),
                () -> verify(executionContextProvider).provide(),
                () -> verify(fileRepository).store(
                        new EncryptedFileInfo(
                                inputFile.fileIdentifier(),
                                inputFile.filename(),
                                inputFile.contentType(),
                                inputFile.contentLength(),
                                uploadedAt,
                                new EncryptedUploadedBy(new ExecutedByEncoded("EU:bobEncoded"), inputFile.ownedBy()),
                                inputFile.ownedBy(),
                                new EncryptedFileMetadata(Encrypted.of("encryptedFileMetadata".getBytes()), inputFile.ownedBy()),
                                new EncryptedCustomMetadata(Encrypted.of("encryptedCustomMetadata".getBytes()), inputFile.ownedBy())
                        ),
                        encrypted
                ),
                () -> verify(fileSizeLimitedCopier).copy(any(), any(), eq(ContentLength.MAX.contentLength())),
                () -> verify(usernameEncoder).encode(any(), any()),
                () -> verify(fileMetadataEncryption).encrypt(any(), any()),
                () -> verify(customMetadataEncryption).encrypt(any(), any())
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
        final FileMetadata fileMetadata = fileMetadata();
        final CustomMetadata customMetadata = customMetadata();
        final UploadedAt uploadedAt = uploadedAt();
        final Encrypted<InputStream> encrypted = encrypted();

        final FileRepositoryException exception = new FileRepositoryException(new IllegalStateException("Database unavailable"));
        when(fileRepository.exists(inputFile.fileIdentifier())).thenReturn(false);
        when(imageMetadataExtractor.extract(any(InputStream.class), eq(inputFile.contentType()))).thenReturn(fileMetadata);
        when(encryptionService.<InputStream>encrypt(any(InputStream.class), eq(inputFile.ownedBy()), any())).thenReturn(encrypted);
        when(executionContextProvider.provide()).thenReturn(executionContext());
        when(uploadedAtProvider.provide()).thenReturn(uploadedAt);
        when(usernameEncoder.encode(new Username("bob@mail.com"), inputFile.ownedBy())).thenReturn(new UsernameEncoded("bobEncoded"));
        when(fileMetadataEncryption.encrypt(fileMetadata, inputFile.ownedBy())).thenReturn(
                new EncryptedFileMetadata(Encrypted.of("encryptedFileMetadata".getBytes()), inputFile.ownedBy()));
        when(customMetadataEncryption.encrypt(customMetadata, inputFile.ownedBy())).thenReturn(
                new EncryptedCustomMetadata(Encrypted.of("encryptedCustomMetadata".getBytes()), inputFile.ownedBy()));
        doThrow(exception).when(fileRepository).store(any(EncryptedFileInfo.class), eq(encrypted));

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(inputFile))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isSameAs(exception),
                () -> verify(fileRepository).store(any(EncryptedFileInfo.class), eq(encrypted)),
                () -> verify(usernameEncoder).encode(any(), any()),
                () -> verify(fileMetadataEncryption).encrypt(any(), any()),
                () -> verify(customMetadataEncryption).encrypt(any(), any())
        );
    }

    @Test
    void shouldWrapMetadataEncryptionExceptionWhenEncryptingFileMetadata() throws Exception {
        // Given
        final InputFile inputFile = inputFile();
        final FileMetadata fileMetadata = fileMetadata();
        final UploadedAt uploadedAt = uploadedAt();
        final Encrypted<InputStream> encrypted = encrypted();

        final MetadataEncryptionException exception = new MetadataEncryptionException(new EncryptionException(new IllegalStateException("Encryption failed")));
        when(fileRepository.exists(inputFile.fileIdentifier())).thenReturn(false);
        when(imageMetadataExtractor.extract(any(InputStream.class), eq(inputFile.contentType()))).thenReturn(fileMetadata);
        when(encryptionService.<InputStream>encrypt(any(InputStream.class), eq(inputFile.ownedBy()), any())).thenReturn(encrypted);
        when(executionContextProvider.provide()).thenReturn(executionContext());
        when(uploadedAtProvider.provide()).thenReturn(uploadedAt);
        when(usernameEncoder.encode(new Username("bob@mail.com"), inputFile.ownedBy())).thenReturn(new UsernameEncoded("bobEncoded"));
        doThrow(exception).when(fileMetadataEncryption).encrypt(fileMetadata, inputFile.ownedBy());

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(inputFile))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isSameAs(exception),
                () -> verify(fileRepository).exists(any()),
                () -> verify(usernameEncoder).encode(any(), any()),
                () -> verify(fileMetadataEncryption).encrypt(any(), any()),
                () -> verifyNoInteractions(customMetadataEncryption)
        );
    }

    @Test
    void shouldWrapMetadataEncryptionExceptionWhenEncryptingCustomMetadata() throws Exception {
        // Given
        final InputFile inputFile = inputFile();
        final FileMetadata fileMetadata = fileMetadata();
        final CustomMetadata customMetadata = customMetadata();
        final UploadedAt uploadedAt = uploadedAt();
        final Encrypted<InputStream> encrypted = encrypted();

        final MetadataEncryptionException exception = new MetadataEncryptionException(new EncryptionException(new IllegalStateException("Encryption failed")));
        when(fileRepository.exists(inputFile.fileIdentifier())).thenReturn(false);
        when(imageMetadataExtractor.extract(any(InputStream.class), eq(inputFile.contentType()))).thenReturn(fileMetadata);
        when(encryptionService.<InputStream>encrypt(any(InputStream.class), eq(inputFile.ownedBy()), any())).thenReturn(encrypted);
        when(executionContextProvider.provide()).thenReturn(executionContext());
        when(uploadedAtProvider.provide()).thenReturn(uploadedAt);
        when(usernameEncoder.encode(new Username("bob@mail.com"), inputFile.ownedBy())).thenReturn(new UsernameEncoded("bobEncoded"));

        when(fileMetadataEncryption.encrypt(fileMetadata, inputFile.ownedBy())).thenReturn(
                new EncryptedFileMetadata(Encrypted.of("encryptedFileMetadata".getBytes()), inputFile.ownedBy()));
        doThrow(exception).when(customMetadataEncryption).encrypt(customMetadata, inputFile.ownedBy());

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(inputFile))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isSameAs(exception),
                () -> verify(fileRepository).exists(any()),
                () -> verify(usernameEncoder).encode(any(), any()),
                () -> verify(fileMetadataEncryption).encrypt(any(), any()),
                () -> verify(customMetadataEncryption).encrypt(any(), any())
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

    private CustomMetadata customMetadata() {
        return new CustomMetadata(Map.of("key", "value"));
    }

    public static UploadedAt uploadedAt() {
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
                new ExecutedBy.EndUser(new Username("bob@mail.com")),
                Set.of("backend-user")
        );
    }

    @SuppressWarnings("unchecked")
    private Encrypted<InputStream> encrypted() {
        return mock(Encrypted.class);
    }
}
