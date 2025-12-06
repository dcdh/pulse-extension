package com.damdamdeo.pulse.extension.it;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PulseExtensionResourceTest {

    @Test
    @Order(1)
    void testCreateTodoEndpoint() {
        given()
                .when().post("/pulse-extension/createTodo")
                .then()
                .log().all()
                .statusCode(200)
                .body("id", is("Damien/20"))
                .body("description", is("lorem ipsum"))
                .body("status", is("IN_PROGRESS"))
                .body("important", is(false));
    }

    @Test
    @Order(2)
    void shouldConsumeAsyncEvents() {
        await().atMost(10, TimeUnit.SECONDS).until(() -> given()
                .when().post("/pulse-extension/called")
                .then()
                .log().all()
                .extract()
                .statusCode() == 200);
        given()
                .when().post("/pulse-extension/called")
                .then()
                .log().all()
                .statusCode(200)
                .body("target.name", is("statistics"))
                .body("aggregateRootType.type", is("Todo"))
                .body("aggregateId.id", is("Damien/20"))
                .body("currentVersionInConsumption.version", is(0))
                .body("currentVersionInConsumption.firstEvent", is(true))
                .body("creationDate", notNullValue())
                .body("eventType.type", is("NewTodoCreated"))
                .body("encryptedPayload.payload", notNullValue(String.class))
                .body("ownedBy.id", is("Damien"))
                .body("decryptableEventPayload.payload.description", is("lorem ipsum"))
                .body("decryptableEventPayload.decrypted", is(true))
                .body("aggregateRootLoaded.aggregateRootType.type", is("Todo"))
                .body("aggregateRootLoaded.aggregateId.id", is("Damien/20"))
                .body("aggregateRootLoaded.lastAggregateVersion.version", is(0))
                .body("aggregateRootLoaded.encryptedAggregateRootPayload.payload", notNullValue(String.class))
                .body("aggregateRootLoaded.decryptableAggregateRootPayload.payload.id.user", is("Damien"))
                .body("aggregateRootLoaded.decryptableAggregateRootPayload.payload.id.sequence", is(20))
                .body("aggregateRootLoaded.decryptableAggregateRootPayload.payload.description", is("lorem ipsum"))
                .body("aggregateRootLoaded.decryptableAggregateRootPayload.payload.status", is("IN_PROGRESS"))
                .body("aggregateRootLoaded.decryptableAggregateRootPayload.payload.important", is(false))
                .body("aggregateRootLoaded.decryptableAggregateRootPayload.decrypted", is(true))
                .body("aggregateRootLoaded.ownedBy.id", is("Damien"))
                .body("aggregateRootLoaded.belongsTo.aggregateId.id", is("Damien/20"));
    }
}
