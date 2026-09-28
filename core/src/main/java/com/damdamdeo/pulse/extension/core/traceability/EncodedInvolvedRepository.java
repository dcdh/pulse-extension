package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;

public interface EncodedInvolvedRepository {

    Page<EncodedInvolved> findBy(AggregateId aggregateId, IncludeUncompounded includeUncompounded, Pagination pagination) throws TraceRepositoryException;

    Page<EncodedInvolved> findBy(ExecutedByHashed executedByHashed, Pagination pagination) throws TraceRepositoryException;
}
