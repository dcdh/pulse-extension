package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import io.quarkus.arc.Unremovable;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.inject.Singleton;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Singleton
@Unremovable
public final class KafkaConnectorApiExecutor {

    private final KafkaConnectorApi kafkaConnectorApi;

    public KafkaConnectorApiExecutor(final DebeziumConfiguration debeziumConfiguration) {
        this.kafkaConnectorApi = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("http://%s:%d".formatted(
                        debeziumConfiguration.connect().host(),
                        debeziumConfiguration.connect().port())))
                .build(KafkaConnectorApi.class);
    }

    public List<String> getAllConnectors() {
        return kafkaConnectorApi.getAllConnectors();
    }

    public CreatedConnectorResponseDTO registerConnector(final KafkaConnectorConfigurationDTO connectorConfiguration) {
        Objects.requireNonNull(connectorConfiguration);
        return kafkaConnectorApi.registerConnector(connectorConfiguration);
    }

    public KafkaConnectorStatusDTO connectorStatus(final String connectorName) {
        Objects.requireNonNull(connectorName);
        return kafkaConnectorApi.connectorStatus(connectorName);
    }
}
