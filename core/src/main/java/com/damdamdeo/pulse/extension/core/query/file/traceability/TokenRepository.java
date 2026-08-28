package com.damdamdeo.pulse.extension.core.query.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;

import java.util.List;

public interface TokenRepository {

    void store(EncryptedTraceability encryptedTraceability) throws TokenRepositoryException;

    List<EncryptedTraceability> listByFileIdentifierOrderByDownloadedAtAsc(FileIdentifier fileIdentifier)
            throws TokenRepositoryException;
}
