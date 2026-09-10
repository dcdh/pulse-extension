package com.damdamdeo.pulse.extension.core.traceability;

// FCK il me faut 2 impelmentations jdbc en fonction de TracingMode
public interface TraceRecorderRepository {

    void store(TraceRecorder traceRecorder) throws TraceRepositoryException;
}
