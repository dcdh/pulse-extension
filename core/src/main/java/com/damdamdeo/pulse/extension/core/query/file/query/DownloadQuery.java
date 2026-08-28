package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.encryption.Decrypted;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionException;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByFactory;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UnableToDecodeException;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.query.file.EncryptedFileInfo;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.FileRepository;
import com.damdamdeo.pulse.extension.core.query.file.FileRepositoryException;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.FiligraneApplier;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.UnableToApplyFiligraneException;
import com.damdamdeo.pulse.extension.core.query.file.traceability.TokenApplier;
import com.damdamdeo.pulse.extension.core.query.file.traceability.TokenApplierException;

import java.util.List;
import java.util.Objects;

public final class DownloadQuery implements GenericQuery<DownloadInput, FileContent> {

    private final FileRepository fileRepository;
    private final ExecutionContextProvider executionContextProvider;
    private final ExecutedByResolver executedByResolver;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;
    private final DecryptionService decryptionService;
    private final FiligraneApplier filigraneApplier;
    private final TokenApplier tokenApplier;
    private final ExecutedByFactory executedByFactory;

    public DownloadQuery(final FileRepository fileRepository,
                         final ExecutionContextProvider executionContextProvider,
                         final ExecutedByResolver executedByResolver,
                         final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                         final DecryptionService decryptionService,
                         final FiligraneApplier filigraneApplier,
                         final TokenApplier tokenApplier,
                         final ExecutedByFactory executedByFactory) {
        this.fileRepository = Objects.requireNonNull(fileRepository);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.executedByResolver = Objects.requireNonNull(executedByResolver);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
        this.decryptionService = Objects.requireNonNull(decryptionService);
        this.filigraneApplier = Objects.requireNonNull(filigraneApplier);
        this.tokenApplier = Objects.requireNonNull(tokenApplier);
        this.executedByFactory = Objects.requireNonNull(executedByFactory);
    }

    @Override
    public FileContent execute(final DownloadInput downloadInput) throws QueryException {
        Objects.requireNonNull(downloadInput);
        try {
            final ExecutionContext provided = executionContextProvider.provide();
            final List<String> visibilityRoles = backendUserVisibilityRolesProvider.provide();
            final EncryptedFileInfo encryptedFileInfo = fileRepository.getFileInfoByFileIdentifier(downloadInput.fileIdentifier());
            final ExecutedBy executedByFromUploader = executedByFactory.from(encryptedFileInfo.encryptedUploadedBy().executedByEncoded().encoded(), encryptedFileInfo.encryptedUploadedBy().ownedBy());
            final boolean uploader = executedByFromUploader.equals(provided.executedBy());
            if (!uploader
                    && !provided.hasAnyRole(visibilityRoles)
                    && executedByResolver.resolve(encryptedFileInfo.ownedBy()).stream().noneMatch(executedByEligible -> provided.executedBy().equals(executedByEligible))) {
                throw new QueryException(new UnauthorizedException());
            }
            final FileContent fileContent = fileRepository.getFileContentByFileIdentifier(downloadInput.fileIdentifier());
            final Decrypted<FileContent> decryptedFileContent = decryptionService.decrypt(Encrypted.of(fileContent.content(), fileContent.contentLength().contentLength()), encryptedFileInfo.ownedBy(),
                    decrypted -> new Decrypted<>(
                            new FileContent(
                                    fileContent.id(),
                                    fileContent.contentType(),
                                    fileContent.contentLength(),
                                    decrypted.payload())
                    ));
            if (provided.hasAnyRole(visibilityRoles)) {
                final FileContent filigranedFileContent = filigraneApplier.apply(decryptedFileContent.payload());
                return tokenApplier.apply(filigranedFileContent, encryptedFileInfo.ownedBy());
            } else {
                return tokenApplier.apply(decryptedFileContent.payload(), encryptedFileInfo.ownedBy());
            }
        } catch (final FileRepositoryException | UnableToDecodeException | UnableToResolveException
                       | DecryptionException | TokenApplierException | UnableToApplyFiligraneException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }
}
