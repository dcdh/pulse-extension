package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record TodoId(String user, Long sequence) implements AggregateId {

    public TodoId {
        Objects.requireNonNull(user);
        Objects.requireNonNull(sequence);
    }

    public String id() {
        return user + "/" + sequence;
    }
}
