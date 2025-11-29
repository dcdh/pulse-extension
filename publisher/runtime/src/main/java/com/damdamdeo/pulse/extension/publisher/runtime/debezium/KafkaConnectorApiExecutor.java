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
                .baseUri(URI.create("http://localhost:%d".formatted(debeziumConfiguration.connect().port())))
                .build(KafkaConnectorApi.class);
    }

    public List<ConnectorNaming> getAllConnectors() {
        return kafkaConnectorApi.getAllConnectors().stream().map(ConnectorNaming::new).toList();
    }

    public CreatedConnectorResponseDTO registerConnector(final KafkaConnectorConfigurationDTO connectorConfiguration) {
        Objects.requireNonNull(connectorConfiguration);
        return kafkaConnectorApi.registerConnector(connectorConfiguration);
    }

    public KafkaConnectorStatusDTO connectorStatus(final ConnectorNaming connectorNaming) {
        Objects.requireNonNull(connectorNaming);
        return kafkaConnectorApi.connectorStatus(connectorNaming.name());
    }
}
