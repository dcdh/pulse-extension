package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.UsernameDecoder;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UnableToDecodeException;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.query.file.*;

import java.util.List;
import java.util.Objects;

public final class GetFileInfoQuery implements GenericQuery<FileIdentifier, FileInfo> {

    private final FileRepository fileRepository;
    private final ExecutionContextProvider executionContextProvider;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;
    private final UsernameDecoder usernameDecoder;
    private final FileMetadataEncryption fileMetadataEncryption;
    private final CustomMetadataEncryption customMetadataEncryption;

    public GetFileInfoQuery(final FileRepository fileRepository,
                            final ExecutionContextProvider executionContextProvider,
                            final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                            final UsernameDecoder usernameDecoder,
                            final FileMetadataEncryption fileMetadataEncryption,
                            final CustomMetadataEncryption customMetadataEncryption) {
        this.fileRepository = Objects.requireNonNull(fileRepository);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
        this.usernameDecoder = Objects.requireNonNull(usernameDecoder);
        this.fileMetadataEncryption = Objects.requireNonNull(fileMetadataEncryption);
        this.customMetadataEncryption = Objects.requireNonNull(customMetadataEncryption);
    }

    @Override
    public FileInfo execute(final FileIdentifier fileIdentifier) throws QueryException {
        Objects.requireNonNull(fileIdentifier);
        try {
            final ExecutionContext provided = executionContextProvider.provide();
            final List<String> visibilityRoles = backendUserVisibilityRolesProvider.provide();
            if (!provided.hasAnyRole(visibilityRoles)) {
                throw new QueryException(new UnauthorizedException());
            }
            final EncryptedFileInfo encryptedFileInfo = fileRepository.getFileInfoByFileIdentifier(fileIdentifier);
            return new FileInfo(
                    encryptedFileInfo.fileIdentifier(),
                    encryptedFileInfo.filename(),
                    encryptedFileInfo.contentType(),
                    encryptedFileInfo.contentLength(),
                    encryptedFileInfo.uploadedAt(),
                    new UploadedBy(encryptedFileInfo.encryptedUploadedBy().executedByEncoded().to(
                            usernameDecoder, encryptedFileInfo.ownedBy())),
                    encryptedFileInfo.ownedBy(),
                    fileMetadataEncryption.decrypt(encryptedFileInfo.encryptedFileMetadata()),
                    customMetadataEncryption.decrypt(encryptedFileInfo.encryptedCustomMetadata())
            );
        } catch (final FileRepositoryException | UnableToDecodeException | MetadataEncryptionException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }
}
