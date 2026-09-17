package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

import java.util.Objects;

public record Actor(ExecutedByHashed executedByHashed, ExecutedBy executedBy) {

    public Actor {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(executedBy);
    }
}
