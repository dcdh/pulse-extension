package com.damdamdeo.pulse.extension.core.encryption;

import java.util.Arrays;
import java.util.Objects;

public record Decrypted<T>(T payload) {

    public Decrypted {
        Objects.requireNonNull(payload);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Decrypted<?>(Object decrypted))) {
            return false;
        }

        if (payload instanceof byte[] bytes1 && decrypted instanceof byte[] bytes2) {
            return Arrays.equals(bytes1, bytes2);
        }

        return Objects.equals(payload, decrypted);
    }

    @Override
    public int hashCode() {
        if (payload instanceof byte[] bytes) {
            return Arrays.hashCode(bytes);
        }

        return Objects.hashCode(payload);
    }
}
