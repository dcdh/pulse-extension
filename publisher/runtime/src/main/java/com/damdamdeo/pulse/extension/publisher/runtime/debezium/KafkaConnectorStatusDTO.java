package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import java.util.List;
import java.util.Objects;

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
