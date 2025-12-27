package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import java.util.List;
import java.util.Set;

public final class InvalidTopicsException extends RuntimeException {

    private final Set<InvalidTopic> invalidTopics;

    public InvalidTopicsException(final Set<InvalidTopic> invalidTopics) {
        this.invalidTopics = invalidTopics;
    }
}
