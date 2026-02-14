package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.writer.runtime.BusinessExceptionToHttpProblemDetailMapper;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

class BusinessExceptionMapperTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @ApplicationScoped
    public static final class DefaultBusinessExceptionToHttpProblemDetailMapper implements BusinessExceptionToHttpProblemDetailMapper {

        @Override
        public String toDetail(final BusinessException exception) {
            return switch (exception.getCause()) {
                case MyException e -> "MyException with context detail";
                default -> "BusinessException with context detail";
            };
        }
    }

    @Path("/businessException")
    public static class BusinessExceptionResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("myException")
        public String myException() throws BusinessException {
            throw new BusinessException(new MyException());
        }
    }

    public static final class MyException extends RuntimeException {

    }

    @Test
    void shouldDefine409BusinessException() {
        getOpenApi()
                .body("paths['/businessException/myException']['get']['responses']['409'].description", equalTo("BusinessException"))
                .body("paths['/businessException/myException']['get']['responses']['409']['content']['application/problem+json'].schema.$ref", equalTo("#/components/schemas/HttpProblem"));
    }

    @Test
    void shouldReturnExpectedResponseOnException() {
        given().contentType(MediaType.TEXT_PLAIN)
                .get("/businessException/myException")
                .then()
                .log().all()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("title", equalTo("Conflict"))
                .body("detail", equalTo("MyException with context detail"))
                .body("instance", equalTo("/businessException/myException"));
    }

    private static ValidatableResponse getOpenApi() {
        return given()
                .accept(ContentType.JSON)
                .get("/q/openapi")
                .then()
                .log().all()
                .statusCode(200);
    }
}
