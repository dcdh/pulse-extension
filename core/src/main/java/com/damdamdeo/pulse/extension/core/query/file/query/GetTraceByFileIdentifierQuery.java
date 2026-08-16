package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.traceability.TokenRepository;
import com.damdamdeo.pulse.extension.core.query.file.traceability.TokenRepositoryException;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Traceability;

import java.util.List;
import java.util.Objects;

public final class GetTraceByFileIdentifierQuery implements GenericQuery<FileIdentifier, List<Traceability>> {

    private final TokenRepository tokenRepository;
    private final ExecutionContextProvider executionContextProvider;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;

    public GetTraceByFileIdentifierQuery(final TokenRepository tokenRepository,
                                         final ExecutionContextProvider executionContextProvider,
                                         final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider) {
        this.tokenRepository = Objects.requireNonNull(tokenRepository);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
    }

    @Override
    public List<Traceability> execute(final FileIdentifier fileIdentifier) throws QueryException {
        Objects.requireNonNull(fileIdentifier);
        try {
            final ExecutionContext provided = executionContextProvider.provide();
            final List<String> visibilityRoles = backendUserVisibilityRolesProvider.provide();
            if (!provided.hasAnyRole(visibilityRoles)) {
                throw new QueryException(new UnauthorizedException());
            }
            return tokenRepository.listByFileIdentifierOrderByDownloadedAtAsc(fileIdentifier);
        } catch (final TokenRepositoryException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }
}
