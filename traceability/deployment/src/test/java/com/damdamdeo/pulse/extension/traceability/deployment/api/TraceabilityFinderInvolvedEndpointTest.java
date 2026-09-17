package com.damdamdeo.pulse.extension.traceability.deployment.api;

import io.quarkus.test.QuarkusUnitTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsMapWithSize.anEmptyMap;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TraceabilityFinderInvolvedEndpointTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubInvolvedFinder.class))
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED")
            .withConfigurationResource("application.properties");

    @Test
    void shouldFindByAggregateId() {
        given()
                .pathParam("aggregateId", "U000001-T000001")
                .queryParam("includeUncompounded" , "true")
                .queryParam("page[index]", "0")
                .queryParam("page[size]", "10")
                .when()
                .get("/traceability/finder/involved/byAggregateId/{aggregateId}")
                .then()
                .log().all()
                .statusCode(200)
                .body("listOfInvolved.size()", equalTo(2))
                .body("listOfInvolved[0].aggregateId", equalTo("U000001-T000001"))
                .body("listOfInvolved[0].executedByHashed", equalTo("EU:alice-hashed"))
                .body("listOfInvolved[0].executedBy", equalTo("EU:alice@mail.com"))
                .body("listOfInvolved[1].aggregateId", equalTo("U000001-T000001"))
                .body("listOfInvolved[1].executedByHashed", equalTo("EU:bob-hashed"))
                .body("listOfInvolved[1].executedBy", equalTo("EU:bob@mail.com"))
                .body("totalPages", equalTo(1))
                .body("hasNext", equalTo(false))
                .body("hasPrevious", equalTo(false));
    }

    @Test
    void shouldMapOnException() {
        given()
                .pathParam("aggregateId", "BOOM")
                .queryParam("includeUncompounded" , "true")
                .queryParam("page[index]", "0")
                .queryParam("page[size]", "10")
                .when()
                .get("/traceability/finder/involved/byAggregateId/{aggregateId}")
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
                .body("instance", equalTo("%2Ftraceability%2Ffinder%2Finvolved%2FbyAggregateId%2FBOOM"))
                .body("parameters", anEmptyMap())
                .body("headers", anEmptyMap());
    }

    @Test
    void shouldFindByExecutedByHashed() {
        given()
                .pathParam("executedByHashed", "EU:alice-hashed")
                .queryParam("page[index]", "0")
                .queryParam("page[size]", "10")
                .when()
                .get("/traceability/finder/involved/byExecutedByHashed/{executedByHashed}")
                .then()
                .log().all()
                .statusCode(200)
                .body("listOfInvolved.size()", equalTo(1))
                .body("listOfInvolved[0].aggregateId", equalTo("U000001-T000001"))
                .body("listOfInvolved[0].executedByHashed", equalTo("EU:alice-hashed"))
                .body("listOfInvolved[0].executedBy", equalTo("EU:alice@mail.com"))
                .body("totalPages", equalTo(1))
                .body("hasNext", equalTo(false))
                .body("hasPrevious", equalTo(false));
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
                    "components": {
                        "schemas": {
                            "AnyAggregateId": {
                                "type": "object",
                                "properties": {
                                    "id": {
                                        "type": "string"
                                    }
                                }
                            },
                            "ExecutedByHashed": {
                                "type": "object",
                                "properties": {
                                    "hashed": {
                                        "type": "string"
                                    }
                                }
                            },
                            "HttpProblem": {
                                "type": "object",
                                "additionalProperties": true,
                                "description": "HTTP Problem Response according to RFC9457 and RFC7807",
                                "properties": {
                                    "type": {
                                        "type": "string",
                                        "format": "uri",
                                        "examples": [
                                            "https://example.com/errors/not-found"
                                        ],
                                        "description": "A optional URI reference that identifies the problem type"
                                    },
                                    "title": {
                                        "type": "string",
                                        "examples": [
                                            "Not Found"
                                        ],
                                        "description": "A optional, short, human-readable summary of the problem type"
                                    },
                                    "status": {
                                        "type": "integer",
                                        "format": "int32",
                                        "examples": [
                                            404
                                        ],
                                        "description": "The HTTP status code for this occurrence of the problem"
                                    },
                                    "detail": {
                                        "type": "string",
                                        "examples": [
                                            "Record not found"
                                        ],
                                        "description": "A optional human-readable explanation specific to this occurrence of the problem"
                                    },
                                    "instance": {
                                        "type": "string",
                                        "format": "uri",
                                        "examples": [
                                            "https://api.example.com/errors/123"
                                        ],
                                        "description": "A URI reference that identifies the specific occurrence of the problem"
                                    }
                                }
                            },
                            "HttpValidationProblem": {
                                "type": "object",
                                "additionalProperties": true,
                                "description": "HTTP Validation Problem Response according to RFC9457 and RFC7807",
                                "properties": {
                                    "type": {
                                        "type": "string",
                                        "format": "uri",
                                        "examples": [
                                            "https://example.com/errors/not-found"
                                        ],
                                        "description": "A optional URI reference that identifies the problem type"
                                    },
                                    "title": {
                                        "type": "string",
                                        "examples": [
                                            "Not Found"
                                        ],
                                        "description": "A optional, short, human-readable summary of the problem type"
                                    },
                                    "status": {
                                        "type": "integer",
                                        "format": "int32",
                                        "examples": [
                                            404
                                        ],
                                        "description": "The HTTP status code for this occurrence of the problem"
                                    },
                                    "detail": {
                                        "type": "string",
                                        "examples": [
                                            "Record not found"
                                        ],
                                        "description": "A optional human-readable explanation specific to this occurrence of the problem"
                                    },
                                    "instance": {
                                        "type": "string",
                                        "format": "uri",
                                        "examples": [
                                            "https://api.example.com/errors/123"
                                        ],
                                        "description": "A URI reference that identifies the specific occurrence of the problem"
                                    },
                                    "violations": {
                                        "type": "array",
                                        "items": {
                                            "$ref": "#/components/schemas/Violation"
                                        },
                                        "description": "List of validation constraint violations that occurred"
                                    }
                                }
                            },
                            "IncludeUncompounded": {
                                "type": "object",
                                "properties": {
                                    "included": {
                                        "type": "boolean"
                                    }
                                }
                            },
                            "InvolvedDTO": {
                                "type": "object",
                                "required": [
                                    "aggregateId",
                                    "executedByHashed",
                                    "executedBy",
                                    "nbOfTimes"
                                ],
                                "description": "Actor involved in the execution of an aggregate.",
                                "properties": {
                                    "aggregateId": {
                                        "type": "string",
                                        "description": "Identifier of the aggregate."
                                    },
                                    "executedByHashed": {
                                        "type": "string",
                                        "description": "Hashed identifier of the actor who executed the operation."
                                    },
                                    "executedBy": {
                                        "type": "string",
                                        "description": "Information identifying the actor who executed the operation."
                                    },
                                    "nbOfTimes": {
                                        "type": [
                                            "integer",
                                            "number"
                                        ],
                                        "format": "int32",
                                        "description": "Nombre of times the actor has been involved."
                                    }
                                }
                            },
                            "InvolvedPageDTO": {
                                "type": "object",
                                "required": [
                                    "listOfInvolved",
                                    "totalPages",
                                    "hasNext",
                                    "hasPrevious"
                                ],
                                "description": "Page of involved actors associated with aggregates.",
                                "properties": {
                                    "listOfInvolved": {
                                        "type": "array",
                                        "items": {
                                            "$ref": "#/components/schemas/InvolvedDTO"
                                        },
                                        "description": "List of involved actors."
                                    },
                                    "totalPages": {
                                        "type": "integer",
                                        "format": "int32",
                                        "description": "Total number of pages."
                                    },
                                    "hasNext": {
                                        "type": "boolean",
                                        "description": "Whether another page is available after the current page."
                                    },
                                    "hasPrevious": {
                                        "type": "boolean",
                                        "description": "Whether a page is available before the current page."
                                    }
                                }
                            },
                            "Violation": {
                                "type": "object",
                                "description": "Validation constraint violation details",
                                "properties": {
                                    "field": {
                                        "type": "string",
                                        "examples": [
                                            "#/profile/email"
                                        ],
                                        "description": "The field for which the validation failed"
                                    },
                                    "in": {
                                        "type": "string",
                                        "examples": [
                                            "query",
                                            "path",
                                            "header",
                                            "form",
                                            "body"
                                        ],
                                        "description": "Part of the http request where the validation error occurred such as query, path, header, form, body"
                                    },
                                    "message": {
                                        "type": "string",
                                        "examples": [
                                            "Invalid email format"
                                        ],
                                        "description": "Description of the validation error"
                                    }
                                }
                            }
                        }
                    },
                    "paths": {
                        "/traceability/finder/involved/byAggregateId/{aggregateId}": {
                            "get": {
                                "parameters": [
                                    {
                                        "name": "aggregateId",
                                        "in": "path",
                                        "required": true,
                                        "schema": {
                                            "$ref": "#/components/schemas/AnyAggregateId"
                                        }
                                    },
                                    {
                                        "name": "includeUncompounded",
                                        "in": "query",
                                        "schema": {
                                            "allOf": [
                                                {
                                                    "$ref": "#/components/schemas/IncludeUncompounded"
                                                },
                                                {
                                                    "default": "false"
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        "required": true,
                                        "schema": {
                                            "type": "integer"
                                        },
                                        "name": "page[index]",
                                        "in": "query"
                                    },
                                    {
                                        "required": true,
                                        "schema": {
                                            "type": "integer"
                                        },
                                        "name": "page[size]",
                                        "in": "query"
                                    }
                                ],
                                "responses": {
                                    "200": {
                                        "description": "OK",
                                        "content": {
                                            "application/json": {
                                                "schema": {
                                                    "$ref": "#/components/schemas/InvolvedPageDTO"
                                                }
                                            }
                                        }
                                    }
                                },
                                "summary": "Find By",
                                "tags": [
                                    "Traceability Finder Involved Endpoint"
                                ]
                            }
                        },
                        "/traceability/finder/involved/byExecutedByHashed/{executedByHashed}": {
                            "get": {
                                "parameters": [
                                    {
                                        "name": "executedByHashed",
                                        "in": "path",
                                        "required": true,
                                        "schema": {
                                            "$ref": "#/components/schemas/ExecutedByHashed"
                                        }
                                    },
                                    {
                                        "required": true,
                                        "schema": {
                                            "type": "integer"
                                        },
                                        "name": "page[index]",
                                        "in": "query"
                                    },
                                    {
                                        "required": true,
                                        "schema": {
                                            "type": "integer"
                                        },
                                        "name": "page[size]",
                                        "in": "query"
                                    }
                                ],
                                "responses": {
                                    "200": {
                                        "description": "OK",
                                        "content": {
                                            "application/json": {
                                                "schema": {
                                                    "$ref": "#/components/schemas/InvolvedPageDTO"
                                                }
                                            }
                                        }
                                    }
                                },
                                "summary": "Find By",
                                "tags": [
                                    "Traceability Finder Involved Endpoint"
                                ]
                            }
                        }
                    },
                    "info": {
                        "title": "TodoTaking API",
                        "version": "1.0.0-SNAPSHOT"
                    }
                }
                """, actual, JSONCompareMode.STRICT);
    }
}
