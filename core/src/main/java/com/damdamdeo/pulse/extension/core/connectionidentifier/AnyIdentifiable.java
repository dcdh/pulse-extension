package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;

public record AnyIdentifiable(String id) implements Identifiable {

    public AnyIdentifiable {
        Objects.requireNonNull(id);
    }

    public static AnyIdentifiable from(final Identifiable identifiable) {
        return new AnyIdentifiable(identifiable.id());
    }
}
