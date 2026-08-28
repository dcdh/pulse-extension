package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByFactory;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UnableToDecodeException;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.traceability.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GetTraceByFileIdentifierQuery implements GenericQuery<FileIdentifier, List<Traceability>> {

    private final TokenRepository tokenRepository;
    private final ExecutionContextProvider executionContextProvider;
    private final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider;
    private final ExecutedByFactory executedByFactory;

    public GetTraceByFileIdentifierQuery(final TokenRepository tokenRepository,
                                         final ExecutionContextProvider executionContextProvider,
                                         final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                                         final ExecutedByFactory executedByFactory) {
        this.tokenRepository = Objects.requireNonNull(tokenRepository);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.backendUserVisibilityRolesProvider = Objects.requireNonNull(backendUserVisibilityRolesProvider);
        this.executedByFactory = Objects.requireNonNull(executedByFactory);
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
            final List<EncryptedTraceability> encryptedTraceabilityData = tokenRepository.listByFileIdentifierOrderByDownloadedAtAsc(fileIdentifier);
            final List<Traceability> traceabilityData = new ArrayList<>(encryptedTraceabilityData.size());
            for (final EncryptedTraceability encryptedTraceability : encryptedTraceabilityData) {
                traceabilityData.add(new Traceability(
                        encryptedTraceability.token(),
                        encryptedTraceability.fileIdentifier(),
                        new DownloadedBy(executedByFactory.from(
                                encryptedTraceability.encryptedDownloadedBy().executedByEncoded().encoded(),
                                encryptedTraceability.encryptedDownloadedBy().ownedBy())),
                        encryptedTraceability.downloadedAt()
                ));
            }
            return traceabilityData;
        } catch (final UnableToDecodeException | TokenRepositoryException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }
}
