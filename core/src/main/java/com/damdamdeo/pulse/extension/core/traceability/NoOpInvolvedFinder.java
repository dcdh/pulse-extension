package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;

public final class NoOpInvolvedFinder implements InvolvedFinder {

    @Override
    public AggregateIdInvolved findBy(final Identifiable identifiable, final Pagination pagination) throws FinderException {
        throw new FinderException(new UnsupportedOperationException("No-op involved finder"));
    }

    @Override
    public ExecutedByInvolved findBy(final ExecutedByHashed executedByHashed, final Pagination pagination) throws FinderException {
        throw new FinderException(new UnsupportedOperationException("No-op involved finder"));
    }
}
