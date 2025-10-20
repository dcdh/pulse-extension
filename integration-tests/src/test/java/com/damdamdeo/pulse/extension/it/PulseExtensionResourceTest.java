package com.damdamdeo.pulse.extension.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.TestMethodOrder;

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
                .body("id", is("00000000-0000-0002-0000-000000000000"))
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
                .body("id", is("00000000-0000-0002-0000-000000000000"))
                .body("description", is("lorem ipsum"))
                .body("status", is("DONE"))
                .body("important", is(false));
    }
}
