package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

public final class NoOpDetailedInvolvedFinder implements DetailedInvolvedFinder {

    @Override
    public Page<DetailedInvolved> findBy(final AggregateId aggregateId, final Pagination pagination) throws FinderException {
        throw new FinderException(new UnsupportedOperationException("No-op involved finder"));
    }

    @Override
    public Page<DetailedInvolved> findBy(final ExecutedByHashed executedByHashed, final Pagination pagination) throws FinderException {
        throw new FinderException(new UnsupportedOperationException("No-op involved finder"));
    }
}
