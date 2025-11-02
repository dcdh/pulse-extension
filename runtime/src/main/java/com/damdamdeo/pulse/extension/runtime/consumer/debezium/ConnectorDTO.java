package com.damdamdeo.pulse.extension.runtime.consumer.debezium;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Objects;

@RegisterForReflection
public record ConnectorDTO(String state, @JsonProperty("worker_id") String workerId) {

    public ConnectorDTO {
        Objects.requireNonNull(state);
        Objects.requireNonNull(workerId);
    }
}
