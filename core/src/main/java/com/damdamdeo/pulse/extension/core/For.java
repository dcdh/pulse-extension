package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;

public record For<A extends Identifiable>(Class<A> identifiableClazz, BelongsTo belongsTo) {

    public For {
        Objects.requireNonNull(identifiableClazz);
        Objects.requireNonNull(belongsTo);
    }
}
