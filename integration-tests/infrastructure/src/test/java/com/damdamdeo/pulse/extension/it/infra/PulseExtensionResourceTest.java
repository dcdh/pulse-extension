package com.damdamdeo.pulse.extension.it.infra;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PulseExtensionResourceTest {

    private static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @Test
    @Order(1)
    void testCreateTodoEndpoint() {
        // Given
        final String accessToken = getAccessToken();

        // When && Then
        given()
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .when()
                .log().all()
                .post("/pulse-extension/creationalWorkflow")
                .then().log().all()
                .statusCode(200)
                // sequences
                .body("sequences", hasSize(0))
                // sequencesTable
                .body("sequencesTable", hasSize(3))
                .body("sequencesTable",
                        contains(
                                "TodoTaking|TodoChecklistId|U000001-T000001|1",
                                "TodoTaking|TodoId|himself|1",
                                "TodoTaking|UserId|himself|1"))
                // databaseConnectionIdentifiers
                .body("databaseConnectionIdentifiers", hasSize(1))
                .body("databaseConnectionIdentifiers[0]",
                        equalTo("d05761c6486e77a8efdb4c5149f84ef0b20abd2454f66a91d7cbd52d71201976|U000001"))
                // aggregateRoots
                .body("aggregateRoots", hasSize(3))
                .body("aggregateRoots", hasItems(
                        "U000001|User|0|U000001|U000001",
                        "U000001-T000001|Todo|0|U000001|U000001",
                        "U000001-T000001-CL000001|TodoChecklist|0|U000001|U000001-T000001"
                ))
                // events
                .body("events", hasSize(3))
                .body("events", hasItems(
                        "U000001|User|0|UserRegistered|U000001|U000001",
                        "U000001-T000001|Todo|0|NewTodoCreated|U000001|U000001",
                        "U000001-T000001-CL000001|TodoChecklist|0|TodoItemAdded|U000001|U000001-T000001"
                ))
                // vaultKeys
                .body("vaultKeys", hasSize(2))
                .body("vaultKeys", hasItems(
                        "/owner/",
                        "/owner/825262468b4cb777358139eafbdec2e0477f898202d8cab60ae9c3a8e79a0de9"));
    }

    @Test
    @Order(2)
    void shouldListConnectedUserTodos() {
        // Given
        final String accessToken = getAccessToken();

        // When && Then
        given()
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .when().get("/pulse-extension/listConnectedUserTodos")
                .then()
                .log().all()
                .statusCode(200)
                .body("projections.size()", is(1))
                .body("projections[0].todoId", matchesPattern(UUID_PATTERN))
                .body("projections[0].description", is("lorem ipsum"))
                .body("projections[0].status", is("IN_PROGRESS"))
                .body("projections[0].important", is(false))
                .body("projections[0].checklist[0].todoChecklistId", matchesPattern(UUID_PATTERN))
                .body("projections[0].checklist[0].description", is("Make it works !"))
                .body("first.todoId", matchesPattern(UUID_PATTERN))
                .body("first.description", is("lorem ipsum"))
                .body("first.status", is("IN_PROGRESS"))
                .body("first.important", is(false))
                .body("first.checklist[0].todoChecklistId", matchesPattern(UUID_PATTERN))
                .body("first.checklist[0].description", is("Make it works !"))
                .body("aggregateIds.size()", is(2))
                .body("aggregateIds[0]", matchesPattern(UUID_PATTERN))
                .body("aggregateIds[1]", matchesPattern(UUID_PATTERN));
    }

    @Test
    @Order(3)
    void shouldConsumeAsyncEvents() {
        await().atMost(10, TimeUnit.SECONDS).until(() -> given()
                .when().post("/pulse-extension/called")
                .then()
                .log().all()
                .extract()
                .statusCode() == 200);
        given()
                .when().post("/pulse-extension/called")
                .then()
                .log().all()
                .statusCode(200)
                // fromApplication
                .body("fromApplication.applicationNaming.name", equalTo("TodoTaking"))
                // purpose
                .body("purpose.name", equalTo("statistics"))
                // aggregate root
                .body("aggregateRootType.type", equalTo("User"))
                .body("aggregateId", matchesPattern(UUID_PATTERN))
                // version
                .body("currentVersionInConsumption.version", equalTo(0))
                // storedAt
                .body("storedAt", notNullValue())
                // event
                .body("eventType.type", equalTo("UserRegistered"))
                // encrypted payload
                .body("encryptedPayload.payload", not(emptyOrNullString()))
                // ownership
                .body("ownedBy.id", equalTo("U000001"))
                .body("belongsTo.id", equalTo("U000001"))
                // executedBy
                .body("executedBy", equalTo("EU:bob@mail.com"))
                // decryptable event payload
                .body("decryptableEventPayload.decrypted", equalTo(true))
                // aggregateRootLoaded
                .body("aggregateRootLoaded.aggregateRootType.type", equalTo("User"))
                .body("aggregateRootLoaded.aggregateId", matchesPattern(UUID_PATTERN))
                .body("aggregateRootLoaded.lastAggregateVersion.version", equalTo(0))
                // encrypted aggregate payload
                .body("aggregateRootLoaded.encryptedAggregateRootPayload.payload", not(emptyOrNullString()))
                // decryptable aggregate payload
                .body("aggregateRootLoaded.decryptableAggregateRootPayload.decrypted",
                        equalTo(true))
                .body("aggregateRootLoaded.decryptableAggregateRootPayload.payload.id.sequence",
                        equalTo("000001"))
                // aggregate ownership
                .body("aggregateRootLoaded.ownedBy.id", equalTo("U000001"))
                .body("aggregateRootLoaded.belongsTo.id", equalTo("U000001"));
    }

    private String getAccessToken() {
        final String authServerUrl = ConfigProvider.getConfig().getValue("quarkus.oidc.auth-server-url", String.class);
        final String clientId = ConfigProvider.getConfig().getValue("quarkus.oidc.client-id", String.class);
        final String secret = ConfigProvider.getConfig().getValue("quarkus.oidc.credentials.secret", String.class);
        return given()
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
    }
}
