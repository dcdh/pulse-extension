package com.damdamdeo.pulse.extension.core.traceability;

public interface TraceAppender {

    void append(Traceable traceable, ExecutionStatus executionStatus) throws TraceAppenderException;
}
