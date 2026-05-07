package com.damdamdeo.pulse.extension.core.hashing;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record Hash(Algorithm algorithm, String value) {

    public static final String SEPARATOR = ":";

    public Hash {
        Objects.requireNonNull(algorithm);
        Objects.requireNonNull(value);
    }

    public Hash from(final String hash) {
        final String[] split = hash.split(SEPARATOR);
        Validate.validState(split.length == 2);
        return new Hash(Algorithm.valueOf(split[0]), split[1]);
    }

    public String hash() {
        return algorithm.name() + SEPARATOR + value;
    }
}
