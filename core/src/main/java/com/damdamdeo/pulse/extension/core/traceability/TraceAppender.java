package com.damdamdeo.pulse.extension.core.traceability;

public interface TraceAppender {

    void append(Traceable traceable, Source source, From from) throws TraceAppenderException;
}
