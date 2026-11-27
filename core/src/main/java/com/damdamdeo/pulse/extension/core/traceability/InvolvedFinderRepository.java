package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;

public interface InvolvedFinderRepository {

    EncodedAggregateIdInvolved findBy(Identifiable identifiable, Pagination pagination) throws TraceRepositoryException;

    EncodedExecutedByInvolved findBy(ExecutedByHashed executedByHashed, Pagination pagination) throws TraceRepositoryException;
}
