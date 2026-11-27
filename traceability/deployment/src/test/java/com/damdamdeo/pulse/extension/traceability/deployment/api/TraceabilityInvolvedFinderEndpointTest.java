package com.damdamdeo.pulse.extension.traceability.deployment.api;

import io.quarkus.test.QuarkusUnitTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsMapWithSize.anEmptyMap;

class TraceabilityInvolvedFinderEndpointTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClass(StubInvolvedFinder.class))
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @Test
    void shouldFindDetailedByIdentifiable() {
        given()
                .pathParam("identifiable", "U000001-T000001")
                .queryParam("page[index]", "0")
                .queryParam("page[size]", "10")
                .when()
                .get("/traceability/finder/byIdentifiable/{identifiable}")
                .then()
                .log().all()
                .statusCode(200)
                .body("identifiable", equalTo("U000001-T000001"))
                .body("linkToAggregateRootId", equalTo("U000001-T000001"))
                .body("nbOfExecutions.executedBy", contains("EU:alice@mail.com", "EU:bob@mail.com"))
                .body("nbOfExecutions.commandNbOfTimes", contains(1, 1))
                .body("page.listOfInvolved.size()", equalTo(2))
                .body("page.listOfInvolved[0].traceId", equalTo(1))
                .body("page.listOfInvolved[0].nbOfExecutions.commandNbOfTimes", equalTo(1))
                .body("page.listOfInvolved[0].executedByHashed", equalTo("EU:alice-hashed"))
                .body("page.listOfInvolved[0].executedBy", equalTo("EU:alice@mail.com"))
                .body("page.listOfInvolved[0].source", equalTo("COMMAND"))
                .body("page.listOfInvolved[0].executionStatus", equalTo("SUCCESS"))
                .body("page.listOfInvolved[0].from", equalTo("from"))
                .body("page.listOfInvolved[0].executedAt", equalTo("2026-09-06T12:00:00Z"))
                .body("page.listOfInvolved[1].traceId", equalTo(1))
                .body("page.listOfInvolved[1].executedByHashed", equalTo("EU:bob-hashed"))
                .body("page.listOfInvolved[1].executedBy", equalTo("EU:bob@mail.com"))
                .body("page.listOfInvolved[1].source", equalTo("COMMAND"))
                .body("page.listOfInvolved[1].executionStatus", equalTo("SUCCESS"))
                .body("page.listOfInvolved[1].from", equalTo("from"))
                .body("page.listOfInvolved[1].executedAt", equalTo("2026-09-06T12:00:00Z"))
                .body("page.totalPages", equalTo(1))
                .body("page.hasNext", equalTo(false))
                .body("page.hasPrevious", equalTo(false));
    }

    @Test
    void shouldReturnProblemWhenFindingByIdentifiableFails() {
        given()
                .pathParam("identifiable", "BOOM")
                .queryParam("page[index]", "0")
                .queryParam("page[size]", "10")
                .when()
                .get("/traceability/finder/byIdentifiable/{identifiable}")
                .then()
                .log().all()
                .statusCode(500)
                .contentType("application/problem+json")
                .body("detailMessage", equalTo("Internal Server Error"))
                .body("cause", nullValue())
                .body("stackTrace", empty())
                .body("suppressedExceptions", empty())
                .body("type", nullValue())
                .body("title", equalTo("Internal Server Error"))
                .body("statusCode", equalTo(500))
                .body("detail", nullValue())
                .body(
                        "instance",
                        equalTo("%2Ftraceability%2Ffinder%2FbyIdentifiable%2FBOOM")
                )
                .body("parameters", anEmptyMap())
                .body("headers", anEmptyMap());
    }

    @Test
    void shouldFindDetailedByExecutedByHashed() {
        given()
                .pathParam("executedByHashed", "EU:alice-hashed")
                .queryParam("page[index]", "0")
                .queryParam("page[size]", "10")
                .when()
                .get("/traceability/finder/byExecutedByHashed/{executedByHashed}")
                .then()
                .log().all()
                .statusCode(200)
                .body("page.listOfInvolved.size()", equalTo(1))
                .body("page.listOfInvolved[0].traceId", equalTo(1))
                .body("page.listOfInvolved[0].nbOfExecutions.commandNbOfTimes", equalTo(1))
                .body("page.listOfInvolved[0].executedByHashed", equalTo("EU:alice-hashed"))
                .body("page.listOfInvolved[0].executedBy", equalTo("EU:alice@mail.com"))
                .body("page.listOfInvolved[0].from", equalTo("from"))
                .body("page.listOfInvolved[0].executedAt", equalTo("2026-09-06T12:00:00Z"))
                .body("page.totalPages", equalTo(1))
                .body("page.hasNext", equalTo(false))
                .body("page.hasPrevious", equalTo(false));
    }

    @Test
    void shouldReturnExpectedOpenapi() throws JSONException {
        final String actual = given()
                .when()
                .log().all()
                .get("/q/openapi?format=json")
                .then()
                .log().all()
                .statusCode(200)
                .extract().asString();
        JSONAssert.assertEquals("""
                {
                    "openapi": "3.1.0",
                    "paths": {
                        "/traceability/finder/byIdentifiable/{identifiable}": {
                            "get": {
                                "parameters": [
                                    {"name": "identifiable", "in": "path", "required": true},
                                    {"name": "page[index]", "in": "query", "required": true},
                                    {"name": "page[size]", "in": "query", "required": true}
                                ],
                                "responses": {"200": {"content": {"application/json": {
                                    "schema": {"$ref": "#/components/schemas/AggregateIdInvolvedDTO"}
                                }}}}
                            }
                        },
                        "/traceability/finder/byExecutedByHashed/{executedByHashed}": {
                            "get": {
                                "parameters": [
                                    {"name": "executedByHashed", "in": "path", "required": true},
                                    {"name": "page[index]", "in": "query", "required": true},
                                    {"name": "page[size]", "in": "query", "required": true}
                                ],
                                "responses": {"200": {"content": {"application/json": {
                                    "schema": {"$ref": "#/components/schemas/ExecutedByInvolvedDTO"}
                                }}}}
                            }
                        }
                    },
                    "components": {
                        "schemas": {
                            "AggregateIdInvolvedDTO": {"properties": {
                                "identifiable": {"type": "string"},
                                "linkToAggregateRootId": {"type": "string"},
                                "page": {"$ref": "#/components/schemas/DetailedInvolvedPageDTO"},
                                "nbOfExecutions": {"type": "array", "items": {"$ref": "#/components/schemas/NbOfExecutionsDTO"}}
                            }},
                            "ExecutedByInvolvedDTO": {"properties": {
                                "page": {"$ref": "#/components/schemas/DetailedInvolvedPageDTO"}
                            }},
                            "DetailedInvolvedPageDTO": {"properties": {
                                "listOfInvolved": {"type": "array", "items": {"$ref": "#/components/schemas/DetailedInvolvedDTO"}},
                                "totalPages": {"type": "integer"},
                                "hasNext": {"type": "boolean"},
                                "hasPrevious": {"type": "boolean"}
                            }},
                            "DetailedInvolvedDTO": {"properties": {
                                "traceId": {"type": ["integer", "number"], "format": "int64"},
                                "correlationId": {"type": ["integer", "number"], "format": "int64"},
                                "executedByHashed": {"type": "string"},
                                "executedBy": {"type": "string"},
                                "eventType": {"type": "string"},
                                "aggregateVersion": {"type": ["integer", "number"], "format": "int32"},
                                "source": {"type": "string"},
                                "executionStatus": {"type": "string"},
                                "from": {"type": "string"},
                                "executedAt": {"type": "string"},
                                "nbOfExecutions": {"$ref": "#/components/schemas/NbOfExecutionsDTO"}
                            }},
                            "NbOfExecutionsDTO": {"properties": {
                                "executedBy": {"type": "string"},
                                "commandNbOfTimes": {"type": "integer"},
                                "commandUnauthorizedNbOfTimes": {"type": "integer"},
                                "commandBusinessFailedNbOfTimes": {"type": "integer"},
                                "queryNbOfTimes": {"type": "integer"},
                                "queryUnauthorizedNbOfTimes": {"type": "integer"},
                                "traceabilityNbOfTimes": {"type": "integer"}
                            }}
                        }
                    },
                    "info": {"title": "TodoTaking API", "version": "1.0.0-SNAPSHOT"}
                }
                """, actual, JSONCompareMode.LENIENT);
    }
}
