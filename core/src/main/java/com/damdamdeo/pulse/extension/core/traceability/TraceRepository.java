package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;

public interface TraceRepository {

    void store(Trace trace) throws TraceRepositoryException;

    Page<SingleTrace> findBy(AggregateId aggregateId, Pagination pagination) throws TraceRepositoryException;

    Page<SingleTrace> findBy(TracedByHashed tracedByHashed, Pagination pagination) throws TraceRepositoryException;
}
