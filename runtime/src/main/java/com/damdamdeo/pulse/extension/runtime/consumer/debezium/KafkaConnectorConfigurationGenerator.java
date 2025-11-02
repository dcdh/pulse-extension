package com.damdamdeo.pulse.extension.runtime.consumer.debezium;

import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Objects;

@Singleton
@Unremovable
public final class KafkaConnectorConfigurationGenerator {

    private final DebeziumConfiguration debeziumConfiguration;
    private final SchemaNamingProvider schemaNamingProvider;
    private final ConnectorNamingProvider connectorNamingProvider;
    private final String jdbcUsername;
    private final String jdbcPassword;

    public KafkaConnectorConfigurationGenerator(final DebeziumConfiguration debeziumConfiguration,
                                                final SchemaNamingProvider schemaNamingProvider,
                                                final ConnectorNamingProvider connectorNamingProvider,
                                                @ConfigProperty(name = "quarkus.datasource.username") final String jdbcUsername,
                                                @ConfigProperty(name = "quarkus.datasource.password") final String jdbcPassword) {
        this.debeziumConfiguration = Objects.requireNonNull(debeziumConfiguration);
        this.schemaNamingProvider = Objects.requireNonNull(schemaNamingProvider);
        this.connectorNamingProvider = Objects.requireNonNull(connectorNamingProvider);
        this.jdbcUsername = Objects.requireNonNull(jdbcUsername);
        this.jdbcPassword = Objects.requireNonNull(jdbcPassword);
    }

    public KafkaConnectorConfigurationDTO generateConnectorConfiguration() {
        return KafkaConnectorConfigurationDTO
                .newBuilder()
                .withName(connectorNamingProvider.provide().name())
                .withConfig(
                        kafkaConnectorConfigurationConfigDTO
                                .newBuilder()
                                .withSchema(schemaNamingProvider.provide().name())
                                .withDatabaseHostname(debeziumConfiguration.connect().postgres().networkName())
                                .withDatabasePort(debeziumConfiguration.connect().postgres().port())
                                .withDatabaseUser(jdbcUsername)
                                .withDatabasePassword(jdbcPassword)
                                .withDatabaseDbname(debeziumConfiguration.connect().postgres().dbName())
                                .build())
                .build();
    }
}
