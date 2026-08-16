package com.damdamdeo.pulse.extension.query.deployment;


import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.QueryExceptionCode;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

class QueryExceptionMapperTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application.properties");

    @Path("exception")
    public static class ExceptionResource {

        @POST
        @Path("unknow")
        public void unknow() throws QueryException {
            throw new QueryException(new RuntimeException("Something wrong happened"), QueryExceptionCode.UNKNOWN);
        }

        @POST
        @Path("forbidden")
        public void forbidden() throws QueryException {
            throw new QueryException(new RuntimeException("Something wrong happened"), QueryExceptionCode.FORBIDDEN);
        }

        @POST
        @Path("conflict")
        public void conflict() throws QueryException {
            throw new QueryException(new RuntimeException("Something wrong happened"), QueryExceptionCode.CONFLICT);
        }

        @POST
        @Path("failFastConditionNotMet")
        public void failFastConditionNotMet() throws QueryException {
            throw new QueryException(new RuntimeException("Something wrong happened"), QueryExceptionCode.FAIL_FAST_CONDITION_NOT_MET);
        }

        @POST
        @Path("infrastructureFailure")
        public void infrastructureFailure() throws QueryException {
            throw new QueryException(new RuntimeException("Something wrong happened"), QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Test
    void shouldReturnExpectedResponseOnUnknow() {
        given()
                .when()
                .log().all()
                .post("/exception/unknow")
                .then().log().all()
                .statusCode(404)
                .body("status", is(404))
                .body("title", is("Not Found"))
                .body("detail", is("java.lang.RuntimeException: Something wrong happened"))
                .body("instance", is("/exception/unknow"));
    }

    @Test
    void shouldReturnExpectedResponseOnForbidden() {
        given()
                .when()
                .log().all()
                .post("/exception/forbidden")
                .then().log().all()
                .statusCode(403)
                .body("status", is(403))
                .body("title", is("Forbidden"))
                .body("detail", is("java.lang.RuntimeException: Something wrong happened"))
                .body("instance", is("/exception/forbidden"));
    }

    @Test
    void shouldReturnExpectedResponseOnConflict() {
        given()
                .when()
                .log().all()
                .post("/exception/conflict")
                .then().log().all()
                .statusCode(409)
                .body("status", is(409))
                .body("title", is("Conflict"))
                .body("detail", is("java.lang.RuntimeException: Something wrong happened"))
                .body("instance", is("/exception/conflict"));
    }

    @Test
    void shouldReturnExpectedResponseOnFailFastConditionNotMet() {
        given()
                .when()
                .log().all()
                .post("/exception/failFastConditionNotMet")
                .then().log().all()
                .statusCode(400)
                .body("status", is(400))
                .body("title", is("Bad Request"))
                .body("detail", is("java.lang.RuntimeException: Something wrong happened"))
                .body("instance", is("/exception/failFastConditionNotMet"));
    }

    @Test
    void shouldReturnExpectedResponseOnInfrastructureFailure() {
        given()
                .when()
                .log().all()
                .post("/exception/infrastructureFailure")
                .then().log().all()
                .statusCode(500)
                .body("status", is(500))
                .body("title", is("Internal Server Error"))
                .body("detail", is("java.lang.RuntimeException: Something wrong happened"))
                .body("instance", is("/exception/infrastructureFailure"));
    }
}
