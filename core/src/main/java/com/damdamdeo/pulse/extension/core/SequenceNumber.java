package com.damdamdeo.pulse.extension.core;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record SequenceNumber(Long number) {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int BASE = ALPHABET.length;
    private static final int LENGTH = 6;

    public SequenceNumber {
        Objects.requireNonNull(number);
        Validate.validState(number >= 0, "sequence number must be greater than or equal to 0");
        Validate.validState(number < Math.pow(BASE, LENGTH),
                "sequence number exceeds maximum encodable value for %d characters".formatted(LENGTH));
    }

    public String value() {
        long current = number;
        final char[] result = new char[LENGTH];
        for (int i = LENGTH - 1; i >= 0; i--) {
            result[i] = ALPHABET[(int) (current % BASE)];
            current /= BASE;
        }
        return new String(result);
    }
}
