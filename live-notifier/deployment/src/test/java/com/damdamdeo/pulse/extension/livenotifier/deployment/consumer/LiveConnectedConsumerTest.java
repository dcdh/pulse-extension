package com.damdamdeo.pulse.extension.livenotifier.deployment.consumer;

import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.livenotifier.SseConsumer;
import com.damdamdeo.pulse.extension.livenotifier.deployment.AbstractMessagingTest;
import com.damdamdeo.pulse.extension.livenotifier.runtime.LiveNotifierPublisher;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class LiveConnectedConsumerTest extends AbstractMessagingTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.keycloak.devservices.users.alice", "alice")
            .overrideConfigKey("quarkus.keycloak.devservices.users.duke", "duke")
            .overrideConfigKey("quarkus.keycloak.devservices.users.bob", "bob")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion())
            ));

    @Inject
    LiveNotifierPublisher<NewTodoCreated> messagingLiveNotifierPublisher;

    @Inject
    SseConsumer sseConsumer;

    @Test
    void shouldConsumeNotificationUsingSseForBobOnly() throws Exception {
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
                        .formParam("username", "bob")
                        .formParam("password", "bob")
                        .when()
                        .log().all()
                        .post("%s/protocol/openid-connect/token".formatted(authServerUrl))
                        .then()
                        .log().all()
                        .statusCode(200)
                        .extract()
                        .path("access_token");

        final CompletableFuture<List<String>> receivedEvents = sseConsumer.consume(accessToken, Duration.ofSeconds(10));

        // When
        messagingLiveNotifierPublisher.publish("TodoEvents", new NewTodoCreated("bob lorem ipsum"), "bob");
        messagingLiveNotifierPublisher.publish("TodoEvents", new NewTodoCreated("duke lorem ipsum"), "duke");
        messagingLiveNotifierPublisher.publish("TodoEvents", new NewTodoCreated("alice lorem ipsum"), "alice");
        messagingLiveNotifierPublisher.publish("TodoEvents", new NewTodoCreated("bob another lorem ipsum"), "bob");

        // Then
        final List<String> ssePayload = receivedEvents.get(12, TimeUnit.SECONDS);

        assertThat(ssePayload).containsExactly(
                """
                        content-type:application/json
                        event:TodoEvents
                        data:{"description":"bob lorem ipsum"}
                        """,
                """
                        content-type:application/json
                        event:TodoEvents
                        data:{"description":"bob another lorem ipsum"}
                        """);
    }
}
