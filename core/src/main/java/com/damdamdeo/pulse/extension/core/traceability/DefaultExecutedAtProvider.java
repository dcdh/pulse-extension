package com.damdamdeo.pulse.extension.core.traceability;

import java.time.ZonedDateTime;

public final class DefaultExecutedAtProvider implements ExecutedAtProvider {

    @Override
    public ExecutedAt now() {
        return new ExecutedAt(ZonedDateTime.now());
    }
}
