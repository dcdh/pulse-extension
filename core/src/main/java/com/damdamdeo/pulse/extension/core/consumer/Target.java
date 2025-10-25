package com.damdamdeo.pulse.extension.core.consumer;

import java.util.Objects;

public record Target(String name) {

    public Target {
        Objects.requireNonNull(name);
    }
}
