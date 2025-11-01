package com.damdamdeo.pulse.extension.core.consumer;

public record DecryptablePayload<T>(T payload, boolean decrypted) {

    public static <T> DecryptablePayload<T> ofUndecryptable() {
        return new DecryptablePayload<>(null, false);
    }

    public static <T> DecryptablePayload<T> ofDecrypted(T payload) {
        return new DecryptablePayload<>(payload, true);
    }

    public boolean isDecrypted() {
        return decrypted;
    }

    public T payload() {
        if (!decrypted) {
            throw new IllegalStateException("Not decrypted");
        }
        return payload;
    }
}
