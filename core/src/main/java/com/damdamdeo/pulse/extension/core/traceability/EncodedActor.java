package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;

import java.util.Objects;

public record EncodedActor(ExecutedByHashed executedByHashed, ExecutedByEncoded executedByEncoded) {

    public EncodedActor {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(executedByEncoded);
    }
}
