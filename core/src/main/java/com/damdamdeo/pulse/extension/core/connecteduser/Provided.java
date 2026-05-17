package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Objects;

public record Provided<A extends Identifiable>(A identifiable, ConnectedUser connectedUser) {

    public Provided {
        Objects.requireNonNull(connectedUser);
    }

    public static <A extends Identifiable> Provided<A> ofKnown(final A identifiable, final ConnectedUser connectedUser) {
        Objects.requireNonNull(identifiable);
        return new Provided<>(identifiable, connectedUser);
    }

    public static <A extends Identifiable> Provided<A> ofUnknown(final ConnectedUser connectedUser) {
        return new Provided<>(null, connectedUser);
    }

    public boolean isUnknown() {
        return identifiable == null;
    }
}
