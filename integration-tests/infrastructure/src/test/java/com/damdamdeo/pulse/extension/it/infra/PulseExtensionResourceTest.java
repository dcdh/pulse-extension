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
                .body("sequences", hasSize(6))
                .body("sequences", hasItems(
                        "todotaking_todo.seq_customfailingtodoid",
                        "todotaking_todo.seq_customidentifiable",
                        "todotaking_todo.seq_todochecklistid",
                        "todotaking_todo.seq_todoid",
                        "todotaking_todo.seq_useraggregateid",
                        "todotaking_todo.seq_userid"
                ))
                // sequenceByIdentifiableClazzAndOwnedBy
                .body("sequenceByIdentifiableClazzAndOwnedBy", hasSize(1))
                .body("sequenceByIdentifiableClazzAndOwnedBy[0]",
                        equalTo("TodoChecklistId|U000001-T000001|1"))
                // databaseConnectionIdentifiers
                .body("databaseConnectionIdentifiers", hasSize(1))
                .body("databaseConnectionIdentifiers[0]",
                        equalTo("d05761c6486e77a8efdb4c5149f84ef0b20abd2454f66a91d7cbd52d71201976|U000001"))
                // aggregateRoots
                .body("aggregateRoots", hasSize(3))
                .body("aggregateRoots", hasItems(
                        "U000001|User|0|UserId|U000001|U000001",
                        "U000001-T000001|Todo|0|UserId|U000001|U000001",
                        "U000001-T000001-CL000001|TodoChecklist|0|UserId|U000001|U000001-T000001"
                ))
                // events
                .body("events", hasSize(3))
                .body("events", hasItems(
                        "U000001|User|0|UserRegistered|UserId|U000001|U000001",
                        "U000001-T000001|Todo|0|NewTodoCreated|UserId|U000001|U000001",
                        "U000001-T000001-CL000001|TodoChecklist|0|TodoItemAdded|UserId|U000001|U000001-T000001"
                ));
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
                .body("size()", is(1))
                .body("[0].todoId.userId.sequence", is("000001"))
                .body("[0].todoId.sequence", is("000001"))
                .body("[0].description", is("lorem ipsum"))
                .body("[0].status", is("IN_PROGRESS"))
                .body("[0].important", is(false))
                .body("[0].checklist[0].todoChecklistId.todoId.userId.sequence", is("000001"))
                .body("[0].checklist[0].todoChecklistId.todoId.sequence", is("000001"))
                .body("[0].checklist[0].todoChecklistId.sequence", is("000001"))
                .body("[0].checklist[0].description", is("Make it works !"))
        ;
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
                .body("fromApplication.functionalDomain", equalTo("TodoTaking"))
                .body("fromApplication.componentName", equalTo("Todo"))
                // purpose
                .body("purpose.name", equalTo("statistics"))
                // aggregate root
                .body("aggregateRootType.type", equalTo("User"))
                .body("aggregateId.id", equalTo("U000001"))
                // version
                .body("currentVersionInConsumption.version", equalTo(0))
                // storedAt
                .body("storedAt", notNullValue())
                // event
                .body("eventType.type", equalTo("UserRegistered"))
                // encrypted payload
                .body("encryptedPayload.payload", not(emptyOrNullString()))
                // ownership
                .body("ownedBy.id", equalTo("UserId|U000001"))
                .body("belongsTo.id", equalTo("U000001"))
                // executedBy
                .body("executedBy", equalTo("EU:bob@mail.com"))
                // decryptable event payload
                .body("decryptableEventPayload.decrypted", equalTo(true))
                // aggregateRootLoaded
                .body("aggregateRootLoaded.aggregateRootType.type", equalTo("User"))
                .body("aggregateRootLoaded.aggregateId.id", equalTo("U000001"))
                .body("aggregateRootLoaded.lastAggregateVersion.version", equalTo(0))
                // encrypted aggregate payload
                .body("aggregateRootLoaded.encryptedAggregateRootPayload.payload", not(emptyOrNullString()))
                // decryptable aggregate payload
                .body("aggregateRootLoaded.decryptableAggregateRootPayload.decrypted",
                        equalTo(true))
                .body("aggregateRootLoaded.decryptableAggregateRootPayload.payload.id.sequence",
                        equalTo("000001"))
                // aggregate ownership
                .body("aggregateRootLoaded.ownedBy.id", equalTo("UserId|U000001"))
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
