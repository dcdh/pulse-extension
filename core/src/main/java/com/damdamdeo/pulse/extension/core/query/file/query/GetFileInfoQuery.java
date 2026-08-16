package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.FileInfo;
import com.damdamdeo.pulse.extension.core.query.file.FileRepository;
import com.damdamdeo.pulse.extension.core.query.file.FileRepositoryException;

import java.util.List;
import java.util.Objects;

public final class GetFileInfoQuery implements GenericQuery<FileIdentifier, FileInfo> {

    private final FileRepository fileRepository;
    private final ExecutionContextProvider executionContextProvider;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;

    public GetFileInfoQuery(final FileRepository fileRepository,
                            final ExecutionContextProvider executionContextProvider,
                            final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider) {
        this.fileRepository = Objects.requireNonNull(fileRepository);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
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
            return fileRepository.getFileInfoByFileIdentifier(fileIdentifier);
        } catch (final FileRepositoryException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }
}
