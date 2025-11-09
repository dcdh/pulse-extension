package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import com.damdamdeo.pulse.extension.core.consumer.ApplicationNaming;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Objects;

@Singleton
@Unremovable
public final class KafkaConnectorConfigurationGenerator {

    private final DebeziumConfiguration debeziumConfiguration;
    private final ApplicationNamingProvider applicationNamingProvider;
    private final String jdbcUsername;
    private final String jdbcPassword;

    public KafkaConnectorConfigurationGenerator(final DebeziumConfiguration debeziumConfiguration,
                                                final ApplicationNamingProvider applicationNamingProvider,
                                                @ConfigProperty(name = "quarkus.datasource.username") final String jdbcUsername,
                                                @ConfigProperty(name = "quarkus.datasource.password") final String jdbcPassword) {
        this.debeziumConfiguration = Objects.requireNonNull(debeziumConfiguration);
        this.applicationNamingProvider = Objects.requireNonNull(applicationNamingProvider);
        this.jdbcUsername = Objects.requireNonNull(jdbcUsername);
        this.jdbcPassword = Objects.requireNonNull(jdbcPassword);
    }

    public KafkaConnectorConfigurationDTO generateConnectorConfiguration() {
        final ApplicationNaming applicationNaming = applicationNamingProvider.provide();
        return KafkaConnectorConfigurationDTO
                .newBuilder()
                .withName(applicationNaming.value().toLowerCase())
                .withConfig(
                        kafkaConnectorConfigurationConfigDTO
                                .newBuilder()
                                .withSchema(applicationNaming.value().toLowerCase())
                                .withDatabaseHostname(debeziumConfiguration.connect().postgres().networkName())
                                .withDatabasePort(debeziumConfiguration.connect().postgres().port())
                                .withDatabaseUser(jdbcUsername)
                                .withDatabasePassword(jdbcPassword)
                                .withDatabaseDbname(debeziumConfiguration.connect().postgres().dbName())
                                .build())
                .build();
    }
}
