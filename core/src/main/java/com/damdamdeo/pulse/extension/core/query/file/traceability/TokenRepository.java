package com.damdamdeo.pulse.extension.core.query.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;

import java.util.List;

public interface TokenRepository {

    void store(Traceability traceability) throws TokenRepositoryException;

    List<Traceability> listByFileIdentifierOrderByDownloadedAtAsc(FileIdentifier fileIdentifier)
            throws TokenRepositoryException;
}
