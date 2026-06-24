package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.connecteduser.*;
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

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

class ConnectedUserAggregateIdProviderTest extends AbstractWriterTest {

    // cf. QuarkusOidcExecutionContextProviderTest
    // cf. JdbcPostgresConnectionIdentifierRepositoryTest
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.oidc.client-id", "account")
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-rest-jackson", Version.getVersion())
            ));

    @Inject
    DataSource dataSource;

    record UserAggregateId(String id) implements AggregateId {

        UserAggregateId {
            Objects.requireNonNull(id);
        }
    }

    @Path("/provide")
    public static class ConnectedUserProviderEndpoint {

        @Inject
        ConnectedUserProvider connectedUserProvider;

        @GET
        public Response provideUserAggregateId() {
            try {
                final ConnectedUser provide = connectedUserProvider.provide();
                return Response.ok(new UserAggregateId(provide.id())).build();
            } catch (final ConnectedIsAnonymousException | UsernameNotAMailException |
                           ConnectedUserNotAvailableException e) {
                return Response.serverError()
                        .entity(Map.of("error", e.getCause()))
                        .build();
            }
        }
    }

    record DatabaseConnectionIdentifier(String connectionIdentifierHash, String identifiableId) {

        DatabaseConnectionIdentifier {
            Objects.requireNonNull(connectionIdentifierHash);
            Objects.requireNonNull(identifiableId);
        }
    }

    // simple component test
    @Test
    void shouldProvide() {
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
                .get("/provide")
                .then().log().all()
                .statusCode(200)
                .body("id", is("bob@mail.com"));
    }
}
