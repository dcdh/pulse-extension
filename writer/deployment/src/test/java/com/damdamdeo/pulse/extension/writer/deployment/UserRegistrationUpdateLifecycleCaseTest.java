package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.User;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.RegisterUser;
import com.damdamdeo.pulse.extension.core.command.UserUpdateUsername;
import com.damdamdeo.pulse.extension.core.connecteduser.registration.UserRegistrationDomainUseCase;
import com.damdamdeo.pulse.extension.core.connecteduser.update.UserUpdateUserNameUseCase;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserRegistrationUpdateLifecycleCaseTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.oidc.client-id", "account")
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion())
            ));

    @Inject
    DataSource dataSource;

    @Path("/user")
    public static class RegistrationEndpoint {

        @Inject
        UserRegistrationDomainUseCase userRegistrationDomainUseCase;

        @Inject
        UserUpdateUserNameUseCase userUpdateUserNameUseCase;

        @POST
        @Path("register")
        @Produces(MediaType.APPLICATION_JSON)
        public UserDTO register() throws BusinessException {
            final User registered = userRegistrationDomainUseCase.execute(new RegisterUser());
            return UserDTO.from(registered);
        }

        @POST
        @Path("update")
        @Produces(MediaType.APPLICATION_JSON)
        public UserDTO update() throws BusinessException {
            final User updated = userUpdateUserNameUseCase.execute(new UserUpdateUsername(UserId.USER_1));
            return UserDTO.from(updated);
        }
    }

    public record UserDTO(String id, String username) {

        public UserDTO {
            Objects.requireNonNull(id);
            Objects.requireNonNull(username);
        }

        public static UserDTO from(final User user) {
            return new UserDTO(user.id().id(), user.username().username());
        }
    }

    @Test
    @Order(1)
    void shouldRegisterUser() {
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
                .post("/user/register")
                .then().log().all()
                .statusCode(200)
                .body("id", equalTo("U000001"))
                .body("username", equalTo("bob@mail.com"));
        assertThat(listEventsAggregateRootIdAggregateRootTypeEventType(dataSource)).containsExactly("U000001|User|UserRegistered");
    }

    // ok I only bob@gmail.com available - update should be allowed because other fields may have changed
    @Order(2)
    @Test
    void shouldUpdateUser() {
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
                .post("/user/update")
                .then().log().all()
                .statusCode(200)
                .body("id", equalTo("U000001"))
                .body("username", equalTo("bob@mail.com"));
        assertThat(listEventsAggregateRootIdAggregateRootTypeEventType(dataSource)).containsExactly("U000001|User|UserRegistered",
                "U000001|User|UsernameUpdated");
    }
}
