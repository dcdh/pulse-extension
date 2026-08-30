package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

public final class NoOpInvolvedFinder implements InvolvedFinder {

    @Override
    public Page<Involved> findBy(final AggregateId aggregateId, final Pagination pagination) throws FinderException {
        throw new FinderException(new UnsupportedOperationException("No-op involved finder"));
    }

    @Override
    public Page<Involved> findBy(final ExecutedByHashed executedByHashed, final Pagination pagination) throws FinderException {
        throw new FinderException(new UnsupportedOperationException("No-op involved finder"));
    }
}
