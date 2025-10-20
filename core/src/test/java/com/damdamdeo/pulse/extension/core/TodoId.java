package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record TodoId(String owner, Long sequence) implements AggregateId {

    public TodoId {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(sequence);
    }

    public String id() {
        return owner + "/" + sequence;
    }
}
