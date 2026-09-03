package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.UsernameDecoder;
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
import java.util.Optional;
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
    private final UsernameDecoder usernameDecoder = mock(UsernameDecoder.class);
    private final FileMetadataEncryption fileMetadataEncryption = mock(FileMetadataEncryption.class);
    private final CustomMetadataEncryption customMetadataEncryption = mock(CustomMetadataEncryption.class);

    private GetFileInfoQuery query;

    @BeforeEach
    void setUp() {
        query = new GetFileInfoQuery(fileRepository, executionContextProvider, backendUserVisibilityRolesProvider,
                usernameDecoder, fileMetadataEncryption, customMetadataEncryption);
    }

    @Test
    void shouldGetFileInfoWhenUserHasVisibilityRole() throws Exception {
        // Given
        final FileInfo fileInfo = fileInfo();
        final EncryptedFileInfo encryptedFileInfo = encryptedFileInfo();
        final FileIdentifier fileIdentifier = encryptedFileInfo.fileIdentifier();

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser(new Username("bob@mail.com")),
                Set.of("backend-user")
        );

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(fileIdentifier)).thenReturn(encryptedFileInfo);
        when(usernameDecoder.decode(new UsernameEncoded("bobEncoded"), encryptedFileInfo.ownedBy())).thenReturn(new Username("bob@mail.com"));
        when(fileMetadataEncryption.decrypt(encryptedFileInfo.encryptedFileMetadata())).thenReturn(fileInfo.fileMetadata());
        when(customMetadataEncryption.decrypt(encryptedFileInfo.encryptedCustomMetadata())).thenReturn(fileInfo.customMetadata());

        // When
        final FileInfo result = query.execute(fileIdentifier);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(fileInfo),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verify(fileRepository).getFileInfoByFileIdentifier(fileIdentifier),
                () -> verify(usernameDecoder).decode(any(), any()),
                () -> verify(fileMetadataEncryption).decrypt(any()),
                () -> verify(customMetadataEncryption).decrypt(any())
        );
    }

    @Test
    void shouldGetFileInfoWhenUserHasOneOfVisibilityRoles() throws Exception {
        // Given
        final FileInfo fileInfo = fileInfo();
        final EncryptedFileInfo encryptedFileInfo = encryptedFileInfo();
        final FileIdentifier fileIdentifier = encryptedFileInfo.fileIdentifier();

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser(new Username("bob@mail.com")),
                Set.of("user", "backend-user", "admin")
        );

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide())
                .thenReturn(List.of("backend-user", "super-admin"));
        when(fileRepository.getFileInfoByFileIdentifier(fileIdentifier)).thenReturn(encryptedFileInfo);
        when(usernameDecoder.decode(new UsernameEncoded("bobEncoded"), encryptedFileInfo.ownedBy())).thenReturn(new Username("bob@mail.com"));
        when(fileMetadataEncryption.decrypt(encryptedFileInfo.encryptedFileMetadata())).thenReturn(fileInfo.fileMetadata());
        when(customMetadataEncryption.decrypt(encryptedFileInfo.encryptedCustomMetadata())).thenReturn(fileInfo.customMetadata());

        // When
        final FileInfo result = query.execute(fileIdentifier);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(fileInfo),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verify(fileRepository).getFileInfoByFileIdentifier(fileIdentifier),
                () -> verify(usernameDecoder).decode(any(), any()),
                () -> verify(fileMetadataEncryption).decrypt(any()),
                () -> verify(customMetadataEncryption).decrypt(any())
        );
    }

    @Test
    void shouldRejectWhenUserDoesNotHaveVisibilityRole() {
        // Given
        final FileIdentifier fileIdentifier = new FileIdentifier("file-123");

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser(new Username("bob@mail.com")),
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
                () -> verifyNoInteractions(fileRepository, usernameDecoder, fileMetadataEncryption,
                        customMetadataEncryption)
        );
    }

    @Test
    void shouldWrapFileRepositoryException() throws Exception {
        // Given
        final EncryptedFileInfo encryptedFileInfo = encryptedFileInfo();
        final FileIdentifier fileIdentifier = encryptedFileInfo.fileIdentifier();
        final FileRepositoryException repositoryException =
                new FileRepositoryException(new IllegalStateException("Database unavailable"));

        final ExecutionContext executionContext = new ExecutionContext(
                new ExecutedBy.EndUser(new Username("bob@mail.com")),
                Set.of("backend-user")
        );

        when(executionContextProvider.provide()).thenReturn(executionContext);
        when(backendUserVisibilityRolesProvider.provide()).thenReturn(List.of("backend-user"));
        when(fileRepository.getFileInfoByFileIdentifier(fileIdentifier)).thenThrow(repositoryException);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> query.execute(fileIdentifier))
                        .isInstanceOf(QueryException.class)
                        .hasFieldOrPropertyWithValue("queryExceptionCode", QueryExceptionCode.INFRASTRUCTURE_FAILURE)
                        .cause()
                        .isInstanceOf(FileRepositoryException.class)
                        .isSameAs(repositoryException),
                () -> verify(executionContextProvider).provide(),
                () -> verify(backendUserVisibilityRolesProvider).provide(),
                () -> verify(fileRepository).getFileInfoByFileIdentifier(any()),
                () -> verifyNoInteractions(usernameDecoder, fileMetadataEncryption, customMetadataEncryption)
        );
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
                        ).toInstant()
                ),
                new UploadedBy(new ExecutedBy.EndUser(new Username("bob@mail.com"))),
                OwnedBy.from(identifier),
                new FileMetadata(
                        Map.of(
                                "author", List.of("BOB"),
                                "tag", List.of("invoice")
                        )
                ),
                new CustomMetadata(Map.of("key", "value"))
        );
    }

    private EncryptedFileInfo encryptedFileInfo() {
        final FileIdentifier identifier = new FileIdentifier("file-123");
        return new EncryptedFileInfo(
                identifier,
                new Filename("facture.jpg"),
                ContentType.IMAGE_JPG,
                new ContentLength(287759L),
                new UploadedAt(
                        ZonedDateTime.of(
                                LocalDate.of(2026, 8, 5),
                                LocalTime.of(23, 0, 31),
                                ZoneOffset.UTC
                        ).toInstant()
                ),
                new EncryptedUploadedBy(new ExecutedByEncoded("EU:bobEncoded"), OwnedBy.from(identifier)),
                OwnedBy.from(identifier),
                new EncryptedFileMetadata(Encrypted.of("encryptedFileMetadata".getBytes()), OwnedBy.from(identifier)),
                new EncryptedCustomMetadata(Encrypted.of("encryptedCustomMetadata".getBytes()), OwnedBy.from(identifier))
        );
    }
}
