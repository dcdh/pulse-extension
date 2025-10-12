package com.damdamdeo.pulse.extension.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PulseExtensionResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/pulse-extension")
                .then()
                .statusCode(200)
                .body(is("Hello pulse-extension"));
    }
}
