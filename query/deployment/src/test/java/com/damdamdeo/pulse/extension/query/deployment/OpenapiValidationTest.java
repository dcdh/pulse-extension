package com.damdamdeo.pulse.extension.query.deployment;

import io.quarkus.test.QuarkusUnitTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static io.restassured.RestAssured.given;

class OpenapiValidationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application.properties");

    @Test
    void shouldReturnExpectedExceptionOnOpenapi() throws JSONException {
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
                            "ContentDisposition": {
                                "type": "string",
                                "enum": [
                                    "INLINE",
                                    "ATTACHMENT"
                                ]
                            },
                            "FileIdentifier": {
                                "type": "object",
                                "properties": {
                                    "id": {
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
                        "/file/{fileIdentifier}/download": {
                            "get": {
                                "parameters": [
                                    {
                                        "name": "fileIdentifier",
                                        "in": "path",
                                        "required": true,
                                        "schema": {
                                            "$ref": "#/components/schemas/FileIdentifier"
                                        }
                                    },
                                    {
                                        "name": "contentDisposition",
                                        "in": "query",
                                        "schema": {
                                            "allOf": [
                                                {
                                                    "$ref": "#/components/schemas/ContentDisposition"
                                                },
                                                {
                                                    "default": "INLINE"
                                                }
                                            ]
                                        }
                                    }
                                ],
                                "responses": {
                                    "500": {
                                        "description": "Internal Server Error",
                                        "content": {
                                            "application/problem+json": {
                                                "schema": {
                                                    "$ref": "#/components/schemas/HttpProblem"
                                                }
                                            }
                                        }
                                    }
                                },
                                "summary": "Download",
                                "tags": [
                                    "File Endpoint"
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
