package com.damdamdeo.pulse.extension.test.consumer.debezium;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class DebeziumConfiguratorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-messaging-kafka", Version.getVersion())))
            .withConfigurationResource("application.properties");

    @Inject
    @ConfigProperty(name = "pulse.debezium.connect.port")
    int pulseDebeziumConnectPort;

    @Test
    void shouldCreateConnector() throws Exception {
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
