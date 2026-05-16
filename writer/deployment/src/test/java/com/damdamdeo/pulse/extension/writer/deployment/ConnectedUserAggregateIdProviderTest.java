package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedUserAggregateIdProvider;
import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedUserAggregateIdProviderException;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

class ConnectedUserAggregateIdProviderTest {

    // cf. QuarkusOidcExecutionContextProviderTest
    // cf. JdbcPostgresConnectionIdentifierRepositoryTest
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addAsResource("realm-quarkus.json"))
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "true")
            .overrideConfigKey("quarkus.devservices.enabled", "true")
            .overrideConfigKey("quarkus.keycloak.devservices.realm-path", "realm-quarkus.json")
            .overrideConfigKey("quarkus.oidc.client-id", "account")
            .overrideRuntimeConfigKey("pulse.datasource.init-at-startup", "true")
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
        ConnectedUserAggregateIdProvider connectedUserAggregateIdProvider;

        @GET
        public Response provideUserAggregateId() {
            try {
                final UserAggregateId provide = connectedUserAggregateIdProvider.provide(UserAggregateId.class,
                        (identifiable -> new UserAggregateId(identifiable.id())),
                        (sequenceNumber -> new UserAggregateId("U" + AggregateId.SEPARATOR + sequenceNumber.number())));
                return Response.ok(new UserAggregateId(provide.id())).build();
            } catch (final ConnectedUserAggregateIdProviderException e) {
                return Response.serverError()
                        .entity(Map.of(
                                "error", e.getCause()
                        ))
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
                .body("id", is("U-000001"));

        final List<DatabaseConnectionIdentifier> databaseConnectionIdentifiers = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT connection_identifier_hash AS connection_identifier_hash, identifiable_id AS identifiable_id FROM connection_identifier
                             """);
             final ResultSet rs = ps.executeQuery()) {
            rs.next();
            databaseConnectionIdentifiers.add(new DatabaseConnectionIdentifier(rs.getString("connection_identifier_hash"), rs.getString("identifiable_id")));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        assertThat(databaseConnectionIdentifiers).containsExactly(
                new DatabaseConnectionIdentifier("d05761c6486e77a8efdb4c5149f84ef0b20abd2454f66a91d7cbd52d71201976", "U-000001"));
    }
}
