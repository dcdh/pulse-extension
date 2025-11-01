package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;

public final class UnknownPassphraseException extends RuntimeException {

    private final OwnedBy ownedBy;

    public UnknownPassphraseException(final OwnedBy ownedBy) {
        this.ownedBy = Objects.requireNonNull(ownedBy);
    }

    public OwnedBy ownedBy() {
        return ownedBy;
    }
}
