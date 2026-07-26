package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;
import java.util.Optional;

public final class DecryptionException extends Exception {

    private final OwnedBy ownedBy;

    public DecryptionException(final OwnedBy ownedBy, final String message) {
        super(message);
        this.ownedBy = Objects.requireNonNull(ownedBy);
    }

    public DecryptionException(final OwnedBy ownedBy, final Throwable cause) {
        super(cause);
        this.ownedBy = Objects.requireNonNull(ownedBy);
    }

    public DecryptionException(final Throwable cause) {
        super(cause);
        ownedBy = null;
    }

    public DecryptionException(final String message) {
        super(message);
        ownedBy = null;
    }

    public Optional<OwnedBy> ownedBy() {
        return Optional.ofNullable(ownedBy);
    }
}
