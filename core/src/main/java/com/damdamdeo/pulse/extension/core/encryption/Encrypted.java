package com.damdamdeo.pulse.extension.core.encryption;

import org.apache.commons.lang3.Validate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

public record Encrypted<T>(T payload, Long size) {

    public Encrypted {
        Objects.requireNonNull(payload);
        Objects.requireNonNull(size);
        Validate.validState(size > 0, "Size must be greater than 0");
    }

    public static Encrypted<byte[]> of(final byte[] payload) {
        return new Encrypted<>(payload, (long) payload.length);
    }

    public static Encrypted<InputStream> of(final ByteArrayInputStream payload) {
        return Encrypted.of(payload, payload.available());
    }

    public static Encrypted<InputStream> of(final InputStream payload, final long size) {
        return new Encrypted<>(payload, size);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Encrypted<?> encrypted)) {
            return false;
        }

        final boolean payloadEquals;
        if (payload instanceof byte[] bytes1 && encrypted.payload() instanceof byte[] bytes2) {
            payloadEquals = Arrays.equals(bytes1, bytes2);
        } else {
            payloadEquals = Objects.equals(payload, encrypted.payload());
        }

        return payloadEquals && Objects.equals(size, encrypted.size());
    }

    @Override
    public int hashCode() {
        final int payloadHashCode = payload instanceof byte[] bytes
                ? Arrays.hashCode(bytes)
                : Objects.hashCode(payload);

        return 31 * payloadHashCode + Objects.hashCode(size);
    }
}
