package com.damdamdeo.pulse.extension.common.runtime.connectionidentifier;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;

public record AnyIdentifiable(String id) implements Identifiable {

    public AnyIdentifiable {
        Objects.requireNonNull(id);
    }
}
