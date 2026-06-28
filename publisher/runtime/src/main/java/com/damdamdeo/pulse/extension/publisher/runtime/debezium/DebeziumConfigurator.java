package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import com.damdamdeo.pulse.extension.core.ApplicationNaming;
import com.damdamdeo.pulse.extension.core.ApplicationNamingProvider;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.faulttolerance.api.TypedGuard;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Singleton
@Unremovable
public final class DebeziumConfigurator {

    final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private static final TypedGuard<Void> GUARD = TypedGuard.create(Void.class)
            .withRetry().delay(1, TimeUnit.SECONDS.toChronoUnit())
            .maxRetries(3)
            .done()
            .build();

    private final DebeziumConfiguration debeziumConfiguration;
    private final ApplicationNamingProvider applicationNamingProvider;
    private final KafkaConnectorConfigurationGenerator kafkaConnectorConfigurationGenerator;
    private final KafkaConnectorApiExecutor kafkaConnectorApiExecutor;

    public DebeziumConfigurator(final DebeziumConfiguration debeziumConfiguration,
                                final ApplicationNamingProvider applicationNamingProvider,
                                final KafkaConnectorConfigurationGenerator kafkaConnectorConfigurationGenerator,
                                final KafkaConnectorApiExecutor kafkaConnectorApiExecutor) {
        this.debeziumConfiguration = Objects.requireNonNull(debeziumConfiguration);
        this.applicationNamingProvider = Objects.requireNonNull(applicationNamingProvider);
        this.kafkaConnectorConfigurationGenerator = Objects.requireNonNull(kafkaConnectorConfigurationGenerator);
        this.kafkaConnectorApiExecutor = Objects.requireNonNull(kafkaConnectorApiExecutor);
    }

    public void onStart(@Observes @Priority(40) final StartupEvent ev) throws Exception {
        if (debeziumConfiguration.enabled()) {
            GUARD.call(() -> {
                final ApplicationNaming applicationNaming = applicationNamingProvider.provide();
                final SchemaName schemaName = SchemaName.from(applicationNaming);
                final List<String> connectors = kafkaConnectorApiExecutor.getAllConnectors();
                if (!connectors.contains(schemaName.name())) {
                    LOGGER.fine("No connector found for " + applicationNaming.name());
                    final KafkaConnectorConfigurationDTO kafkaConnectorConfigurationDTO = kafkaConnectorConfigurationGenerator.generateConnectorConfiguration();
                    LOGGER.info("Configuring connector with " + kafkaConnectorConfigurationDTO.toString());
                    kafkaConnectorApiExecutor.registerConnector(kafkaConnectorConfigurationDTO);
                    boolean isRunning;
                    int i = 0;
                    do {
                        TimeUnit.SECONDS.sleep(2);
                        final KafkaConnectorStatusDTO kafkaConnectorStatusDTO = kafkaConnectorApiExecutor.connectorStatus(applicationNaming.name());
                        isRunning = kafkaConnectorStatusDTO.isRunning();
                        LOGGER.info("Connector status for " + applicationNaming.name() + " is running " + isRunning);
                        i++;
                    } while (!isRunning && i < 10);
                    if (!isRunning) {
                        throw new Exception("Failed to register connector");
                    }
                }
                return null;
            });
        }
    }
}
