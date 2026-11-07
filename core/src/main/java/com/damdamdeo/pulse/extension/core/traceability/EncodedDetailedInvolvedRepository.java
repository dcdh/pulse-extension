package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;

public interface EncodedDetailedInvolvedRepository {

    Page<EncodedDetailedInvolved> findBy(Identifiable identifiable, IncludeUncompounded includeUncompounded, Pagination pagination) throws TraceRepositoryException;

    Page<EncodedDetailedInvolved> findBy(ExecutedByHashed executedByHashed, Pagination pagination) throws TraceRepositoryException;
}
