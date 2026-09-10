package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

public interface EncodedInvolvedRepository {

    Page<EncodedInvolved> findBy(AnyAggregateId aggregateId, Pagination pagination) throws TraceRepositoryException;

    Page<EncodedInvolved> findBy(ExecutedByHashed executedByHashed, Pagination pagination) throws TraceRepositoryException;
}
