package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

public interface EncodedDetailedInvolvedRepository {

    Page<EncodedDetailedInvolved> findBy(AggregateId aggregateId, Pagination pagination) throws TraceRepositoryException;

    Page<EncodedDetailedInvolved> findBy(ExecutedByHashed executedByHashed, Pagination pagination) throws TraceRepositoryException;
}
