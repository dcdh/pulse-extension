package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import java.util.Objects;

public record CreatedConnectorResponseDTO(String name) {

    public CreatedConnectorResponseDTO {
        Objects.requireNonNull(name);
    }
}
