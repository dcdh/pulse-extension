package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import java.util.Objects;

public record InvalidTopic(String name, Integer nbOfPartitions) {

    public InvalidTopic {
        Objects.requireNonNull(name);
        Objects.requireNonNull(nbOfPartitions);
    }
}
