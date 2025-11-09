package com.damdamdeo.pulse.extension.publisher.deployment.debezium;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class DebeziumConfiguratorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @Inject
    @ConfigProperty(name = "pulse.debezium.connect.port")
    int pulseDebeziumConnectPort;

    @Test
    void shouldHaveCreatedConnector() throws Exception {
        // Given

        // When
        // Automatically call at startup via @Observes StartupEvent
        // debeziumConfigurator.onStart(new StartupEvent());

        // Then
        given()
                .baseUri("http://localhost:%d".formatted(pulseDebeziumConnectPort))
                .log().all()
                .when()
                .get("/connectors/todotaking_todo/status")
                .then()
                .log().all()
                .statusCode(200)
                .body("name", is("todotaking_todo"))
                .body("connector.state", is("RUNNING"))
                .body("connector.worker_id", notNullValue())
                .body("connector.version", is("3.3.1.Final"))
                .body("tasks", hasSize(1))
                .body("tasks[0].id", is(0))
                .body("tasks[0].state", is("RUNNING"))
                .body("tasks[0].worker_id", notNullValue())
                .body("tasks[0].version", is("3.3.1.Final"))
                .body("type", is("source"));
    }
}
