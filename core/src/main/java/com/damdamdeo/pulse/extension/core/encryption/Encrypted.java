package com.damdamdeo.pulse.extension.core.encryption;

import java.util.Arrays;
import java.util.Objects;

public record Encrypted<T>(T payload) {

    public Encrypted {
        Objects.requireNonNull(payload);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Encrypted<?>(Object encrypted))) {
            return false;
        }

        if (payload instanceof byte[] bytes1 && encrypted instanceof byte[] bytes2) {
            return Arrays.equals(bytes1, bytes2);
        }

        return Objects.equals(payload, encrypted);
    }

    @Override
    public int hashCode() {
        if (payload instanceof byte[] bytes) {
            return Arrays.hashCode(bytes);
        }

        return Objects.hashCode(payload);
    }
}
