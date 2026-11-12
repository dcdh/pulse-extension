package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record Description(String description) {

    public Description {
        Objects.requireNonNull(description);
    }
}
