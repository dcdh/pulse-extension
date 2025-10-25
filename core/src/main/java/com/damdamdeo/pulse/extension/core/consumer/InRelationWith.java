package com.damdamdeo.pulse.extension.core.consumer;

import java.util.Objects;

public record InRelationWith(String with) {

    public InRelationWith {
        Objects.requireNonNull(with);
    }
}
