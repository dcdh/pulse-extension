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
    void testMarkTodoAsDoneEndpoint() {
        given()
                .when().post("/pulse-extension/markTodoAsDone")
                .then()
                .log().all()
                .statusCode(200)
                .body("id", is("Damien/20"))
                .body("description", is("lorem ipsum"))
                .body("status", is("DONE"))
                .body("important", is(false));
    }

    @Test
    @Order(3)
    void shouldConsumeAsyncEvents() {
        // FCK 1
//        putain passer par le stub et les tester toutes !!!
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            final Boolean hasBeenCalled = given()
                    .when().post("/pulse-extension/hasBeenCalled")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(Boolean.class);
            return Boolean.TRUE.equals(hasBeenCalled);
        });
    }
}
