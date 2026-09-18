package com.damdamdeo.pulse.extension.obfuscator;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusTest
class ObfuscatorResourceTest {

    @Order(1)
    @Test
    void shouldObfuscate() {
        given().when().get("/obfuscator")
                .then()
                .log().all()
                .statusCode(200)
                .body("todoId", is("00000000-0000-0000-0000-000000000000"))
                .body("description", is("lorem ipsum"))
                .body("status", is("IN_PROGRESS"))
                .body("important", is(false));
    }

    @Order(2)
    @Test
    void shouldGetByTodoId() {
        given().when().get("/obfuscator/byAggregateId/U000001-T000001")
                .then()
                .log().all()
                .statusCode(200)
                .body(is("U000001-T000001"));
    }

    @Order(3)
    @Test
    void shouldObfuscateUsingObfuscatedAnnotation() {
        given().when().get("/obfuscator/annotatedProjection")
                .then()
                .log().all()
                .statusCode(200)
                .body("id", is("00000000-0000-0000-0000-000000000000"))
                .body("todoId", is("00000000-0000-0000-0000-000000000000"))
                .body("description", is("lorem ipsum"))
                .body("status", is("IN_PROGRESS"))
                .body("important", is(false));
    }

    @Order(4)
    @Test
    void shouldDeobfuscateParameter() {
        given().when().get("/obfuscator/deObfuscate/00000000-0000-0000-0000-000000000000")
                .then()
                .log().all()
                .statusCode(200)
                .body(is("U000001-T000001"));
    }

    @Order(5)
    @Test
    void shouldDeobfuscateByAggregateIdParameter() {
        given().when().get("/obfuscator/deObfuscateByAggregateId/00000000-0000-0000-0000-000000000000")
                .then()
                .log().all()
                .statusCode(200)
                .body(is("U000001-T000001"));
    }
}
