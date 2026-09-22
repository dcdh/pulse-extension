package com.damdamdeo.pulse.extension.core.traceability;

import java.util.Objects;

public final class NoOpTraceAppender implements TraceAppender {

    @Override
    public void append(final Traceable traceable, final Source source, final From from) throws TraceAppenderException {
        Objects.requireNonNull(traceable);
        Objects.requireNonNull(source);
        Objects.requireNonNull(from);
        // no-op
    }
}
