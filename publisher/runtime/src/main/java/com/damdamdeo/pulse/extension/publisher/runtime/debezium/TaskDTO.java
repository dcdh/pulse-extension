package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Objects;

@RegisterForReflection
public record TaskDTO(Integer id, String state, @JsonProperty("worker_id") String workerId) {

    public TaskDTO {
        Objects.requireNonNull(id);
        Objects.requireNonNull(state);
        Objects.requireNonNull(workerId);
    }
}
