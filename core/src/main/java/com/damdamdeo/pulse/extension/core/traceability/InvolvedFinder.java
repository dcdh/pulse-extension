package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;

public interface InvolvedFinder {

    AggregateIdInvolved findBy(Identifiable identifiable, Pagination pagination) throws FinderException;

    ExecutedByInvolved findBy(ExecutedByHashed executedByHashed, Pagination pagination) throws FinderException;
}
