package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;

public final class DecryptionException extends RuntimeException {

    private final OwnedBy ownedBy;

    public DecryptionException(final OwnedBy ownedBy, final String message) {
        super(message);
        this.ownedBy = Objects.requireNonNull(ownedBy);
    }

    public DecryptionException(final OwnedBy ownedBy, final Throwable cause) {
        super(cause);
        this.ownedBy = Objects.requireNonNull(ownedBy);
    }

    public OwnedBy ownedBy() {
        return ownedBy;
    }
}
