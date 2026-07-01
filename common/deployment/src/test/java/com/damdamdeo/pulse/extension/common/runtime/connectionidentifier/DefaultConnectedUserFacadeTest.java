package com.damdamdeo.pulse.extension.common.runtime.connectionidentifier;

import com.damdamdeo.pulse.extension.common.runtime.StubPassphraseRepository;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.connecteduser.DefaultConnectedUserFacade;
import com.damdamdeo.pulse.extension.core.connectionidentifier.*;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DefaultConnectedUserFacadeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClass(StubPassphraseRepository.class))
            .overrideConfigKey("quarkus.oidc.client-id", "account")
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-rest-jackson", Version.getVersion())
            ));

    @Path("/registration")
    public static class RegistrationEndpoint {

        @Inject
        ConnectionIdentifierRepository connectionIdentifierRepository;

        @Inject
        ConnectionIdentifierProvider connectionIdentifierProvider;

        @Inject
        Hasher hasher;

        @Inject
        DefaultConnectedUserFacade defaultConnectedUserFacade;

        @GET
        @Path("isRegistered")
        public String isRegistered() {
            return defaultConnectedUserFacade.isRegistered().map(Identifiable::id).orElse(null);
        }

        @POST
        @Path("register")
        @Transactional
        public void register() throws ConnectionIdentifierProviderException, ConnectionIdentifierRepositoryException,
                DuplicateConnectionIdentifierException {
            final ConnectionIdentifier connectionIdentifier = connectionIdentifierProvider.provide();
            connectionIdentifierRepository.store(connectionIdentifier, UserId.USER_1);
        }
    }

    @Test
    void shouldReturnFalseWhenNotRegistered() {
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
                .get("/registration/isRegistered")
                .then().log().all()
                .statusCode(204);
    }

    @Test
    void shouldReturnTrueWhenRegistered() {
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

        // When
        given()
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .when()
                .log().all()
                .post("/registration/register")
                .then().log().all()
                .statusCode(204);

        // Then
        given()
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .when()
                .log().all()
                .get("/registration/isRegistered")
                .then().log().all()
                .statusCode(200)
                .body(equalTo("U000001"));
    }
}
