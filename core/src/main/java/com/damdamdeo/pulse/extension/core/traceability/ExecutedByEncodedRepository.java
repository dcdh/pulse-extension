package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

public interface ExecutedByEncodedRepository {

    ExecutedByEncoded findBy(ExecutedByHashed executedByHashed) throws ExecutedByEncodedRepositoryException;

    void store(ExecutedByHashed executedByHashed, ExecutedByEncoded executedByEncoded) throws ExecutedByEncodedRepositoryException;
}
