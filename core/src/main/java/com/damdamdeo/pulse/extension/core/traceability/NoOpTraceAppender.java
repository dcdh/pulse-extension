package com.damdamdeo.pulse.extension.core.traceability;

public final class NoOpTraceAppender implements TraceAppender {

    @Override
    public void append(final Traceable traceable, final From from) throws TraceAppenderException {
        throw new TraceAppenderException(new UnsupportedOperationException("No-op trace appender"));
    }
}
