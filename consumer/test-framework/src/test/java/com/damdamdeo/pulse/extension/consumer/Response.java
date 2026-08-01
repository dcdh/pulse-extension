package com.damdamdeo.pulse.extension.consumer;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;

import java.util.Objects;

public record Response(Encrypted<byte[]> encryptedAggregateRoot, Encrypted<byte[]> encryptedEvent) {

    public Response {
        Objects.requireNonNull(encryptedAggregateRoot);
        Objects.requireNonNull(encryptedEvent);
    }
}
