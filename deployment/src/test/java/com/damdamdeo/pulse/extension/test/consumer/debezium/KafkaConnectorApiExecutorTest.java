package com.damdamdeo.pulse.extension.test.consumer.debezium;

import com.damdamdeo.pulse.extension.runtime.consumer.debezium.*;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KafkaConnectorApiExecutorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-messaging-kafka", Version.getVersion())))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.arc.exclude-types", "com.damdamdeo.pulse.extension.runtime.consumer.debezium.DebeziumConfigurator");

    @Inject
    KafkaConnectorApiExecutor kafkaConnectorApiExecutor;

    @Inject
    KafkaConnectorConfigurationGenerator kafkaConnectorConfigurationGenerator;

    @Test
    @Order(1)
    void shouldRegisterConnector() {
        // Given
        final KafkaConnectorConfigurationDTO givenKafkaConnectorConfigurationDTO = kafkaConnectorConfigurationGenerator.generateConnectorConfiguration();

        // When
        final CreatedConnectorResponseDTO createdConnectorResponseDTO = kafkaConnectorApiExecutor.registerConnector(givenKafkaConnectorConfigurationDTO);

        // Then
        assertThat(createdConnectorResponseDTO).isEqualTo(new CreatedConnectorResponseDTO("todotaking_todo"));
    }

    @Test
    @Order(2)
    void shouldReturnConnectorStatus() {
        // Given

        // When
        final KafkaConnectorStatusDTO kafkaConnectorStatusDTO = kafkaConnectorApiExecutor.connectorStatus(new ConnectorNaming("todotaking_todo"));

        // Then
        assertAll(
                () -> assertThat(kafkaConnectorStatusDTO.name()).isEqualTo("todotaking_todo"),
                () -> assertThat(kafkaConnectorStatusDTO.connector().state()).isEqualTo("RUNNING"),
                () -> assertThat(kafkaConnectorStatusDTO.connector().workerId()).isNotNull(),
                () -> assertThat(kafkaConnectorStatusDTO.connector().version()).isEqualTo("3.3.1.Final"),
                () -> assertThat(kafkaConnectorStatusDTO.tasks().size()).isEqualTo(0),
                () -> assertThat(kafkaConnectorStatusDTO.type()).isEqualTo("source"));
    }

    @Test
    @Order(3)
    void shouldListAllConnectors() {
        // Given

        // When
        final List<ConnectorNaming> allConnectors = kafkaConnectorApiExecutor.getAllConnectors();

        // Then
        assertThat(allConnectors).containsExactly(new ConnectorNaming("todotaking_todo"));
    }
}
