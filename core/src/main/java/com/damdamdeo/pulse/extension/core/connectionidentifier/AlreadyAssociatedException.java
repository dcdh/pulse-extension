package com.damdamdeo.pulse.extension.core.connectionidentifier;

import java.util.Objects;

public class AlreadyAssociatedException extends Exception {

    private final ConnectionIdentifier connectionIdentifier;

    public AlreadyAssociatedException(final ConnectionIdentifier connectionIdentifier) {
        this.connectionIdentifier = Objects.requireNonNull(connectionIdentifier);
    }

    public ConnectionIdentifier connectionIdentifier() {
        return connectionIdentifier;
    }
}
