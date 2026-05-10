package com.damdamdeo.pulse.extension.core.hashing;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record Hash(Algorithm algorithm, String hashed) {

    public Hash {
        Objects.requireNonNull(algorithm);
        Objects.requireNonNull(hashed);
        Validate.validState(algorithm.validationPattern().matcher(hashed).matches());
    }
}
