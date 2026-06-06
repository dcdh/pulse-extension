package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.common.runtime.connecteduser.QuarkusOidcConnectedUserProvider;
import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedIsAnonymousException;
import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedUser;
import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedUserNotAvailableException;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameNotAMailException;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

class QuarkusOidcConnectedUserProviderTest extends AbstractWriterTest {

    // cf. QuarkusOidcExecutionContextProviderTest
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.oidc.client-id", "account")
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-rest-jackson", Version.getVersion())
            ));

    @Path("/connectedUserProvider")
    public static class ConnectedUserProviderEndpoint {

        @Inject
        QuarkusOidcConnectedUserProvider quarkusOidcConnectedUserProvider;

        @GET
        public Response getConnectedUserDTO() {
            try {
                final ConnectedUser connectedUser = quarkusOidcConnectedUserProvider.provide();
                return Response.ok(new ConnectedUserDTO(connectedUser.username().username())).build();
            } catch (final ConnectedIsAnonymousException connectedIsAnonymousException) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of(
                                "error", "anonymous_user",
                                "message", "Connected user is anonymous"))
                        .build();
            } catch (final UsernameNotAMailException usernameNotAMailException) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "error", "missing_email",
                                "message", "Connected user does not have an email"))
                        .build();
            } catch (final ConnectedUserNotAvailableException e) {
                throw new RuntimeException("Should not be here", e);// quarkusOidcConnectedUserProvider will never throw this exception
            }
        }
    }

    public record ConnectedUserDTO(String username) {
    }

    @Test
    void shouldReturnConnectedUserUsingEmailAsUsernameWhenAuthenticated() {
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
                        .formParam("username", "bob@mail.com")
                        .formParam("password", "bob")
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
                .get("/connectedUserProvider")
                .then().log().all()
                .statusCode(200)
                .body("username", is("bob@mail.com"));
    }

    @Test
    void shouldFailOnConnectedUserNotUsingAnEmailAsUsernameWhenAuthenticated() {
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
                .get("/connectedUserProvider")
                .then().log().all()
                .statusCode(400)
                .body("error", is("missing_email"))
                .body("message", is("Connected user does not have an email"));
    }

    @Test
    void shouldFailOnAnonymousUser() {
        given()
                .when()
                .log().all()
                .get("/connectedUserProvider")
                .then().log().all()
                .statusCode(401)
                .body("error", is("anonymous_user"))
                .body("message", is("Connected user is anonymous"));
    }
}
