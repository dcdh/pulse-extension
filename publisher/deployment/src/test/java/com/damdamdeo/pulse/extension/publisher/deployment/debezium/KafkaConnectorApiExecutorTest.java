package com.damdamdeo.pulse.extension.publisher.deployment.debezium;

import com.damdamdeo.pulse.extension.publisher.runtime.debezium.*;
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
class KafkaConnectorApiExecutorTest extends AbstractPublisherTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.arc.exclude-types", DebeziumConfigurator.class.getName());

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
        assertThat(createdConnectorResponseDTO).isEqualTo(new CreatedConnectorResponseDTO("todo_taking"));
    }

    @Test
    @Order(2)
    void shouldReturnConnectorStatus() {
        // Given

        // When
        final KafkaConnectorStatusDTO kafkaConnectorStatusDTO = kafkaConnectorApiExecutor.connectorStatus("todo_taking");

        // Then
        assertAll(
                () -> assertThat(kafkaConnectorStatusDTO.name()).isEqualTo("todo_taking"),
                () -> assertThat(kafkaConnectorStatusDTO.connector().state()).isEqualTo("RUNNING"),
                () -> assertThat(kafkaConnectorStatusDTO.connector().workerId()).isNotNull(),
                () -> assertThat(kafkaConnectorStatusDTO.connector().version()).isEqualTo("3.5.0.Final"),
                () -> assertThat(kafkaConnectorStatusDTO.tasks().size()).isEqualTo(0),
                () -> assertThat(kafkaConnectorStatusDTO.type()).isEqualTo("source"));
    }

    @Test
    @Order(3)
    void shouldListAllConnectors() {
        // Given

        // When
        final List<String> allConnectors = kafkaConnectorApiExecutor.getAllConnectors();

        // Then
        assertThat(allConnectors).containsExactly("todo_taking");
    }
}
