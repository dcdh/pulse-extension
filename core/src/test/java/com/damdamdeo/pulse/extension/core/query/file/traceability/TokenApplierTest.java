package com.damdamdeo.pulse.extension.core.query.file.traceability;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.*;
import com.damdamdeo.pulse.extension.core.query.file.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenApplierTest {

    private final TokenGenerator tokenGenerator = mock(TokenGenerator.class);
    private final TokenRepository tokenRepository = mock(TokenRepository.class);
    private final ExecutionContextProvider executionContextProvider = mock(ExecutionContextProvider.class);
    private final DownloadedAtProvider downloadedAtProvider = mock(DownloadedAtProvider.class);
    private final ExecutedByEncoder executedByEncoder = mock(ExecutedByEncoder.class);
    private final ContentTypeTokenApplier contentTypeTokenApplier = mock(ContentTypeTokenApplier.class);

    private final TokenApplier tokenApplier = new TokenApplier(tokenGenerator, tokenRepository, executionContextProvider,
            downloadedAtProvider, executedByEncoder, List.of(contentTypeTokenApplier));

    @Test
    void shouldApplyToken() throws Exception {
        // Given
        final FileContent fileContent = fileContent();
        final OwnedBy ownedBy = OwnedBy.from(new FileIdentifier("file-123"));
        final Token token = token();
        final DownloadedAt downloadedAt = downloadedAt();
        final FileContent tokenizedFileContent = tokenizedFileContent();

        when(contentTypeTokenApplier.contentTypes()).thenReturn(List.of(ContentType.IMAGE_JPG));
        when(tokenGenerator.generate()).thenReturn(token);
        when(executionContextProvider.provide()).thenReturn(backendUserExecutionContext());
        when(executedByEncoder.encode("ADMIN", ownedBy)).thenReturn(Encrypted.of("ADMIN".getBytes()));
        when(downloadedAtProvider.provide()).thenReturn(downloadedAt);
        when(contentTypeTokenApplier.apply(fileContent, token)).thenReturn(tokenizedFileContent);

        // When
        final FileContent result = tokenApplier.apply(fileContent, ownedBy);

        // Then
        assertAll(
                () -> assertSame(tokenizedFileContent, result),
                () -> verify(contentTypeTokenApplier).contentTypes(),
                () -> verify(tokenGenerator).generate(),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executedByEncoder).encode("ADMIN", ownedBy),
                () -> verify(downloadedAtProvider).provide(),
                () -> verify(tokenRepository).store(
                        new EncryptedTraceability(
                                token,
                                fileContent.id(),
                                new EncryptedDownloadedBy(new ExecutedByEncoded("EU:ADMIN"), ownedBy),
                                downloadedAt
                        )
                ),
                () -> verify(contentTypeTokenApplier).apply(fileContent, token),
                () -> verifyNoMoreInteractions(tokenGenerator, tokenRepository, executionContextProvider,
                        downloadedAtProvider, executedByEncoder, contentTypeTokenApplier)
        );
    }

    @Test
    void shouldWrapUnsupportedContentTypeException() {
        // Given
        final FileContent fileContent = fileContent();
        final OwnedBy ownedBy = OwnedBy.from(new FileIdentifier("file-123"));

        when(contentTypeTokenApplier.contentTypes()).thenReturn(List.of(ContentType.APPLICATION_PDF));

        // When
        final TokenApplierException exception = assertThrows(TokenApplierException.class,
                () -> tokenApplier.apply(fileContent, ownedBy));

        // Then
        assertAll(
                () -> assertInstanceOf(UnableToApplyTokenException.class, exception.getCause()),
                () -> assertInstanceOf(UnsupportedContentTypeException.class, exception.getCause().getCause()),
                () -> verify(contentTypeTokenApplier).contentTypes(),
                () -> verifyNoInteractions(tokenGenerator, tokenRepository, executionContextProvider, downloadedAtProvider,
                        executedByEncoder),
                () -> verifyNoMoreInteractions(contentTypeTokenApplier)
        );
    }

    @Test
    void shouldWrapTokenRepositoryException() throws Exception {
        // Given
        final FileContent fileContent = fileContent();
        final OwnedBy ownedBy = OwnedBy.from(new FileIdentifier("file-123"));
        final Token token = token();
        final DownloadedAt downloadedAt = downloadedAt();
        final TokenRepositoryException cause = new TokenRepositoryException(new IllegalStateException("Database unavailable"));

        when(contentTypeTokenApplier.contentTypes()).thenReturn(List.of(ContentType.IMAGE_JPG));
        when(tokenGenerator.generate()).thenReturn(token);
        when(executionContextProvider.provide()).thenReturn(backendUserExecutionContext());
        when(executedByEncoder.encode("ADMIN", ownedBy)).thenReturn(Encrypted.of("ADMIN".getBytes()));
        when(downloadedAtProvider.provide()).thenReturn(downloadedAt);
        doThrow(cause).when(tokenRepository).store(any(EncryptedTraceability.class));

        // When
        final TokenApplierException exception = assertThrows(TokenApplierException.class,
                () -> tokenApplier.apply(fileContent, ownedBy));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(contentTypeTokenApplier).contentTypes(),
                () -> verify(tokenGenerator).generate(),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executedByEncoder).encode("ADMIN", ownedBy),
                () -> verify(downloadedAtProvider).provide(),
                () -> verify(tokenRepository).store(
                        new EncryptedTraceability(
                                token,
                                fileContent.id(),
                                new EncryptedDownloadedBy(new ExecutedByEncoded("EU:ADMIN"), ownedBy),
                                downloadedAt
                        )
                ),
                () -> verifyNoMoreInteractions(contentTypeTokenApplier, tokenGenerator, tokenRepository, executionContextProvider,
                        downloadedAtProvider, executedByEncoder)
        );
    }

    @Test
    void shouldWrapUnableToEncodeException() throws Exception {
        // Given
        final FileContent fileContent = fileContent();
        final OwnedBy ownedBy = OwnedBy.from(new FileIdentifier("file-123"));
        final Token token = token();
        final UnableToEncodeException cause = new UnableToEncodeException(new IllegalStateException("Unable to encode"));

        when(contentTypeTokenApplier.contentTypes()).thenReturn(List.of(ContentType.IMAGE_JPG));
        when(tokenGenerator.generate()).thenReturn(token);
        when(executionContextProvider.provide()).thenReturn(backendUserExecutionContext());
        when(executedByEncoder.encode("ADMIN", ownedBy)).thenThrow(cause);

        // When
        final TokenApplierException exception = assertThrows(TokenApplierException.class,
                () -> tokenApplier.apply(fileContent, ownedBy));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(contentTypeTokenApplier).contentTypes(),
                () -> verify(tokenGenerator).generate(),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executedByEncoder).encode("ADMIN", ownedBy),
                () -> verifyNoInteractions(tokenRepository, downloadedAtProvider),
                () -> verifyNoMoreInteractions(tokenGenerator, executionContextProvider, executedByEncoder,
                        contentTypeTokenApplier)
        );
    }

    @Test
    void shouldWrapUnableToApplyTokenException() throws Exception {
        // Given
        final FileContent fileContent = fileContent();
        final OwnedBy ownedBy = OwnedBy.from(new FileIdentifier("file-123"));
        final Token token = token();
        final DownloadedAt downloadedAt = downloadedAt();
        final UnableToApplyTokenException cause = new UnableToApplyTokenException(
                new IllegalStateException("Unable to apply token"));

        when(contentTypeTokenApplier.contentTypes()).thenReturn(List.of(ContentType.IMAGE_JPG));
        when(tokenGenerator.generate()).thenReturn(token);
        when(executionContextProvider.provide()).thenReturn(backendUserExecutionContext());
        when(executedByEncoder.encode("ADMIN", ownedBy)).thenReturn(Encrypted.of("ADMIN".getBytes()));
        when(downloadedAtProvider.provide()).thenReturn(downloadedAt);
        when(contentTypeTokenApplier.apply(fileContent, token)).thenThrow(cause);

        // When
        final TokenApplierException exception = assertThrows(TokenApplierException.class,
                () -> tokenApplier.apply(fileContent, ownedBy));

        // Then
        assertAll(
                () -> assertSame(cause, exception.getCause()),
                () -> verify(contentTypeTokenApplier).contentTypes(),
                () -> verify(tokenGenerator).generate(),
                () -> verify(executionContextProvider).provide(),
                () -> verify(executedByEncoder).encode("ADMIN", ownedBy),
                () -> verify(downloadedAtProvider).provide(),
                () -> verify(tokenRepository).store(
                        new EncryptedTraceability(
                                token,
                                fileContent.id(),
                                new EncryptedDownloadedBy(new ExecutedByEncoded("EU:ADMIN"), ownedBy),
                                downloadedAt
                        )
                ),
                () -> verify(contentTypeTokenApplier).apply(fileContent, token),
                () -> verifyNoMoreInteractions(tokenGenerator, tokenRepository, executionContextProvider, downloadedAtProvider,
                        executedByEncoder, contentTypeTokenApplier)
        );
    }

    private Token token() {
        return new Token(UUID.fromString("12345678-1234-1234-1234-123456789012"));
    }

    private DownloadedAt downloadedAt() {
        return new DownloadedAt(
                ZonedDateTime.of(
                        LocalDate.of(2026, 8, 23),
                        LocalTime.of(13, 0, 0),
                        ZoneOffset.UTC
                )
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

    private FileContent tokenizedFileContent() {
        return new FileContent(
                new FileIdentifier("file-123"),
                ContentType.IMAGE_JPG,
                new ContentLength(287759L),
                new ByteArrayInputStream("tokenized-decrypted-content".getBytes())
        );
    }

    private ExecutionContext backendUserExecutionContext() {
        return new ExecutionContext(
                new ExecutedBy.EndUser("ADMIN", true),
                Set.of("backend-user")
        );
    }
}
