package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.encryption.Decrypted;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionException;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.query.file.*;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.FiligraneApplier;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.UnableToApplyFiligraneException;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DownloadQuery implements GenericQuery<DownloadInput, FileContent> {

    private final FileRepository fileRepository;
    private final ExecutionContextProvider executionContextProvider;
    private final ExecutedByResolver executedByResolver;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;
    private final DecryptionService decryptionService;
    private final FiligraneApplier filigraneApplier;

    public DownloadQuery(final FileRepository fileRepository,
                         final ExecutionContextProvider executionContextProvider,
                         final ExecutedByResolver executedByResolver,
                         final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                         final DecryptionService decryptionService,
                         final FiligraneApplier filigraneApplier) {
        this.fileRepository = Objects.requireNonNull(fileRepository);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.executedByResolver = Objects.requireNonNull(executedByResolver);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
        this.decryptionService = Objects.requireNonNull(decryptionService);
        this.filigraneApplier = Objects.requireNonNull(filigraneApplier);
    }

    @Override
    public FileContent execute(final DownloadInput downloadInput) throws QueryException {
        Objects.requireNonNull(downloadInput);
        try {
            final ExecutionContext provided = executionContextProvider.provide();
            final List<String> visibilityRoles = backendUserVisibilityRolesProvider.provide();
            final FileInfo fileInfoByFileIdentifier = fileRepository.getFileInfoByFileIdentifier(downloadInput.fileIdentifier());
            final Set<ExecutedBy> executedByEligibles = executedByResolver.resolve(fileInfoByFileIdentifier.ownedBy());
            if (!provided.hasAnyRole(visibilityRoles)
                    && executedByEligibles.stream().noneMatch(executedByEligible -> provided.executedBy().equals(executedByEligible))) {
                throw new QueryException(new UnauthorizedException());
            }
            final FileContent fileContent = fileRepository.getFileContentByFileIdentifier(downloadInput.fileIdentifier());
            final Decrypted<FileContent> decryptedFileContent = decryptionService.decrypt(Encrypted.of(fileContent.content(), fileContent.contentLength().contentLength()), fileInfoByFileIdentifier.ownedBy(),
                    decrypted -> new Decrypted<>(
                            new FileContent(
                                    fileContent.id(),
                                    fileContent.contentType(),
                                    fileContent.contentLength(),
                                    decrypted.payload())
                    ));
            if (provided.hasAnyRole(visibilityRoles)) {
                return filigraneApplier.apply(decryptedFileContent.payload());
            } else {
                return decryptedFileContent.payload();
            }
        } catch (final FileRepositoryException | UnableToResolveException | DecryptionException
                       | UnableToApplyFiligraneException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }
}
