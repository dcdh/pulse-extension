package com.damdamdeo.pulse.extension.core.encryption;

import java.util.Arrays;
import java.util.Objects;

public record EncryptedPayload(byte[] payload) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EncryptedPayload that = (EncryptedPayload) o;
        return Objects.deepEquals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(payload);
    }

    @Override
    public String toString() {
        return "EncryptedPayload{" +
                "payload=" + Arrays.toString(payload) +
                '}';
    }
}
