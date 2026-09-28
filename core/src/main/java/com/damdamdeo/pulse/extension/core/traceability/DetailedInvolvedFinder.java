package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;

public interface DetailedInvolvedFinder {

    Page<DetailedInvolved> findBy(AggregateId aggregateId, IncludeUncompounded includeUncompounded, Pagination pagination) throws FinderException;

    Page<DetailedInvolved> findBy(ExecutedByHashed executedByHashed, Pagination pagination) throws FinderException;
}
