package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.hashing.Hash;

import java.util.Objects;

public class UnableToFindByHashException extends Exception {
    private final Hash<ConnectionIdentifier> connectionIdentifierHash;

    public UnableToFindByHashException(final Throwable cause, final Hash<ConnectionIdentifier> connectionIdentifierHash) {
        super(cause);
        this.connectionIdentifierHash = Objects.requireNonNull(connectionIdentifierHash);
    }

    public Hash<ConnectionIdentifier> connectionIdentifierHash() {
        return connectionIdentifierHash;
    }
}
