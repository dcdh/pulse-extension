package com.damdamdeo.pulse.extension.core.traceability;

public interface TraceAppender {

    void append(Traceable traceable, Source source, ExecutionStatus executionStatus, From from) throws TraceAppenderException;
}
