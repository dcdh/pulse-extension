package com.damdamdeo.pulse.extension.core.hashing;

import java.util.Objects;

public record Hash(String hashed) {

    public Hash {
        Objects.requireNonNull(hashed);
    }
}
