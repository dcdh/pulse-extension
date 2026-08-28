package com.damdamdeo.pulse.extension.core.encryption;

import org.apache.commons.lang3.Validate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

public interface Encrypted<T> {

    T payload();

    Long size();

    record ByteArrayEncrypted(byte[] payload) implements Encrypted<byte[]> {

        public ByteArrayEncrypted {
            Objects.requireNonNull(payload);
        }

        public Long size() {
            return (long) payload.length;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ByteArrayEncrypted that)) {
                return false;
            }
            return Arrays.equals(payload, that.payload);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(payload);
        }
    }

    record InputStreamEncrypted(InputStream payload, Long size) implements Encrypted<InputStream> {

        public InputStreamEncrypted {
            Objects.requireNonNull(payload);
            Objects.requireNonNull(size);
            Validate.validState(size > 0, "Size must be greater than 0");
        }
    }

    static Encrypted<byte[]> of(final byte[] payload) {
        return new ByteArrayEncrypted(payload);
    }

    static Encrypted<InputStream> of(final ByteArrayInputStream payload) {
        return Encrypted.of(payload, payload.available());
    }

    static Encrypted<InputStream> of(final InputStream payload, final long size) {
        return new InputStreamEncrypted(payload, size);
    }
}
