package com.damdamdeo.pulse.extension.runtime.consumer.debezium;

import java.util.Objects;

public record CreatedConnectorResponseDTO(String name) {

    public CreatedConnectorResponseDTO {
        Objects.requireNonNull(name);
    }
}
