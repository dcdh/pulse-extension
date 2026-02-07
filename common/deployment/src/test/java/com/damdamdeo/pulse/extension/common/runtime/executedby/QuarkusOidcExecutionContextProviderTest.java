package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class QuarkusOidcExecutionContextProviderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.keycloak.devservices.users.alice", "alice")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-rest-jackson", Version.getVersion())
            ));

    @Path("/executedByProvider")
    public static class ExecutedByProviderEndpoint {

        @Inject
        ExecutionContextProvider executionContextProvider;

        @GET
        public ExecutionContextDTO getExecutionContextDTO() {
            final ExecutionContext executionContext = executionContextProvider.provide();
            return new ExecutionContextDTO(
                    executionContext.executedBy().value(),
                    executionContext.roles());
        }
    }

    public record ExecutionContextDTO(String executedBy, Set<String> roles) {
    }

    @Test
    void shouldReturnAliceEndUser() {
        // Given
        final String authServerUrl = ConfigProvider.getConfig().getValue("quarkus.oidc.auth-server-url", String.class);
        final String clientId = ConfigProvider.getConfig().getValue("quarkus.oidc.client-id", String.class);
        final String secret = ConfigProvider.getConfig().getValue("quarkus.oidc.credentials.secret", String.class);
        final String accessToken =
                given()
                        .contentType(ContentType.URLENC)
                        .formParam("grant_type", "password")
                        .formParam("client_id", clientId)
                        .formParam("client_secret", secret)
                        .formParam("username", "alice")
                        .formParam("password", "alice")
                        .when()
                        .log().all()
                        .post("%s/protocol/openid-connect/token".formatted(authServerUrl))
                        .then()
                        .log().all()
                        .statusCode(200)
                        .extract()
                        .path("access_token");

        // When && Then
        given()
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .when()
                .log().all()
                .get("/executedByProvider")
                .then().log().all()
                .statusCode(200)
                .body("executedBy", is("EU:alice"))
                .body("roles", hasItems("admin", "user"));
    }

    @Test
    void shouldReturnServiceAccount() {
        // Given
        final String authServerUrl = ConfigProvider.getConfig().getValue("quarkus.oidc.auth-server-url", String.class);
        final String clientId = ConfigProvider.getConfig().getValue("quarkus.oidc.client-id", String.class);
        final String secret = ConfigProvider.getConfig().getValue("quarkus.oidc.credentials.secret", String.class);
        final String accessToken =
                given()
                        .contentType(ContentType.URLENC)
                        .formParam("grant_type", "client_credentials")
                        .formParam("client_id", clientId)
                        .formParam("client_secret", secret)
                        .when()
                        .post("%s/protocol/openid-connect/token".formatted(authServerUrl))
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("access_token");

        // When && Then
        given()
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .when()
                .log().all()
                .get("/executedByProvider")
                .then().log().all()
                .statusCode(200)
                .body("executedBy", is("SA:service-account-quarkus-app"))
                .body("roles", hasItems("offline_access", "default-roles-quarkus", "uma_authorization"));
    }

    @Test
    void shouldReturnAnonymous() {
        given()
                .when()
                .log().all()
                .get("/executedByProvider")
                .then().log().all()
                .statusCode(200)
                .body("executedBy", is("A"))
                .body("roles", empty());
    }
}
