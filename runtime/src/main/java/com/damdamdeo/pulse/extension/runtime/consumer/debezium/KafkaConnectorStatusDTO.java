package com.damdamdeo.pulse.extension.runtime.consumer.debezium;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;
import java.util.Objects;

@RegisterForReflection
public record KafkaConnectorStatusDTO(String name, ConnectorDTO connector, List<TaskDTO> tasks, String type) {

    private static final String RUNNING = "RUNNING";

    public KafkaConnectorStatusDTO {
        Objects.requireNonNull(name);
        Objects.requireNonNull(connector);
        Objects.requireNonNull(type);
    }

    public boolean isRunning() {
        return RUNNING.equals(connector.state());
    }
}
