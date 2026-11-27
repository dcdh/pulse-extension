package com.damdamdeo.pulse.extension.core.traceability;

public interface TraceRecorderRepository {

    void store(TraceRecorder traceRecorder) throws TraceRepositoryException;

    void store(TraceRecorder traceRecorder, TraceabilityDetails traceabilityDetails) throws TraceRepositoryException;
}
