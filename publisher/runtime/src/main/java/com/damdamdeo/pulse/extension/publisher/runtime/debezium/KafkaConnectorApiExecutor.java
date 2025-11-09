package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import io.quarkus.arc.Unremovable;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.inject.Singleton;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
@Unremovable
public final class KafkaConnectorApiExecutor {

    private final Optional<KafkaConnectorApi> kafkaConnectorApi;

    public KafkaConnectorApiExecutor(final DebeziumConfiguration debeziumConfiguration) {
        this.kafkaConnectorApi = debeziumConfiguration.connect().port().map(
                port -> QuarkusRestClientBuilder.newBuilder()
                        .baseUri(URI.create("http://localhost:%d".formatted(port)))
                        .build(KafkaConnectorApi.class));
    }

    public List<ConnectorNaming> getAllConnectors() {
        return kafkaConnectorApi.map(api -> api.getAllConnectors().stream().map(ConnectorNaming::new).toList())
                .orElseThrow(() -> new IllegalArgumentException("Missing port"));
    }

    public CreatedConnectorResponseDTO registerConnector(final KafkaConnectorConfigurationDTO connectorConfiguration) {
        Objects.requireNonNull(connectorConfiguration);
        return kafkaConnectorApi.map(api -> api.registerConnector(connectorConfiguration))
                .orElseThrow(() -> new IllegalArgumentException("Missing port"));
    }

    public KafkaConnectorStatusDTO connectorStatus(final ConnectorNaming connectorNaming) {
        Objects.requireNonNull(connectorNaming);
        return kafkaConnectorApi.map(api -> api.connectorStatus(connectorNaming.name()))
                .orElseThrow(() -> new IllegalArgumentException("Missing port"));
    }
}
