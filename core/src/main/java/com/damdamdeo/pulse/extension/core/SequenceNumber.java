package com.damdamdeo.pulse.extension.core;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public final class SequenceNumber {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int BASE = ALPHABET.length;
    private static final int LENGTH = 6;

    private final String number;

    public SequenceNumber(final String number) {
        this.number = Objects.requireNonNull(number);
        Validate.validState(number.matches("[0-9A-Z]{%d}".formatted(LENGTH)), "sequence number must be alphanumeric");
    }

    public String number() {
        return number;
    }

    public static SequenceNumber fromNumber(final Long number) {
        Objects.requireNonNull(number);
        long current = number;
        final char[] result = new char[LENGTH];
        for (int i = LENGTH - 1; i >= 0; i--) {
            result[i] = ALPHABET[(int) (current % BASE)];
            current /= BASE;
        }
        return new SequenceNumber(new String(result));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SequenceNumber that = (SequenceNumber) o;
        return Objects.equals(number, that.number);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(number);
    }
}
