package com.damdamdeo.pulse.extension.consumer;

import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;

import java.util.Objects;

public record Response(EncryptedPayload encryptedAggregateRoot, EncryptedPayload encryptedEvent) {

    public Response {
        Objects.requireNonNull(encryptedAggregateRoot);
        Objects.requireNonNull(encryptedEvent);
    }
}
