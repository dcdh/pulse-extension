package com.damdamdeo.pulse.extension.core.executedby;

import java.util.Objects;

public record UsernameHashed(String hashed) {

    public UsernameHashed {
        Objects.requireNonNull(hashed);
    }
}
