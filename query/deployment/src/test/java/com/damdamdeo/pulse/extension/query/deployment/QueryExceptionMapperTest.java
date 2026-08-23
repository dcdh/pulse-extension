package com.damdamdeo.pulse.extension.query.deployment;


import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.QueryExceptionCode;
import io.quarkiverse.resteasy.problem.HttpProblem;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

class QueryExceptionMapperTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application.properties");

    @Path("exception")
    public static class ExceptionResource {

        @APIResponse(
                responseCode = "404",
                description = "Not found",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = HttpProblem.class)
                )
        )
        @POST
        @Path("unknow")
        public void unknow() throws QueryException {
            throw new QueryException(new RuntimeException("Something wrong happened"), QueryExceptionCode.UNKNOWN);
        }

        @APIResponse(
                responseCode = "403",
                description = "Forbidden",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = HttpProblem.class)
                )
        )
        @POST
        @Path("forbidden")
        public void forbidden() throws QueryException {
            throw new QueryException(new RuntimeException("Something wrong happened"), QueryExceptionCode.FORBIDDEN);
        }

        @APIResponse(
                responseCode = "409",
                description = "Conflict",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = HttpProblem.class)
                )
        )
        @POST
        @Path("conflict")
        public void conflict() throws QueryException {
            throw new QueryException(new RuntimeException("Something wrong happened"), QueryExceptionCode.CONFLICT);
        }

        @APIResponse(
                responseCode = "400",
                description = "Bad request",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = HttpProblem.class)
                )
        )
        @POST
        @Path("failFastConditionNotMet")
        public void failFastConditionNotMet() throws QueryException {
            throw new QueryException(new RuntimeException("Something wrong happened"), QueryExceptionCode.FAIL_FAST_CONDITION_NOT_MET);
        }

        @APIResponse(
                responseCode = "500",
                description = "Internal Server Error",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = HttpProblem.class)
                )
        )
        @POST
        @Path("infrastructureFailure")
        public void infrastructureFailure() throws QueryException {
            throw new QueryException(new RuntimeException("Something wrong happened"), QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }

        @POST
        @Path("defaultExceptionOpenapi")
        public void defaultExceptionOpenapi() throws QueryException {
        }
    }

    @Test
    void shouldReturnExpectedResponseOnUnknow() {
        given()
                .when()
                .log().all()
                .post("/exception/unknow")
                .then().log().all()
                .statusCode(404)
                .body("status", is(404))
                .body("title", is("Not Found"))
                .body("detail", is("java.lang.RuntimeException: Something wrong happened"))
                .body("instance", is("/exception/unknow"));
    }

    @Test
    void shouldReturnExpectedResponseOnForbidden() {
        given()
                .when()
                .log().all()
                .post("/exception/forbidden")
                .then().log().all()
                .statusCode(403)
                .body("status", is(403))
                .body("title", is("Forbidden"))
                .body("detail", is("java.lang.RuntimeException: Something wrong happened"))
                .body("instance", is("/exception/forbidden"));
    }

    @Test
    void shouldReturnExpectedResponseOnConflict() {
        given()
                .when()
                .log().all()
                .post("/exception/conflict")
                .then().log().all()
                .statusCode(409)
                .body("status", is(409))
                .body("title", is("Conflict"))
                .body("detail", is("java.lang.RuntimeException: Something wrong happened"))
                .body("instance", is("/exception/conflict"));
    }

    @Test
    void shouldReturnExpectedResponseOnFailFastConditionNotMet() {
        given()
                .when()
                .log().all()
                .post("/exception/failFastConditionNotMet")
                .then().log().all()
                .statusCode(400)
                .body("status", is(400))
                .body("title", is("Bad Request"))
                .body("detail", is("java.lang.RuntimeException: Something wrong happened"))
                .body("instance", is("/exception/failFastConditionNotMet"));
    }

    @Test
    void shouldReturnExpectedResponseOnInfrastructureFailure() {
        given()
                .when()
                .log().all()
                .post("/exception/infrastructureFailure")
                .then().log().all()
                .statusCode(500)
                .body("status", is(500))
                .body("title", is("Internal Server Error"))
                .body("detail", is("java.lang.RuntimeException: Something wrong happened"))
                .body("instance", is("/exception/infrastructureFailure"));
    }

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
                            "ContentType": {
                                "type": "string",
                                "enum": [
                                    "APPLICATION_PDF",
                                    "IMAGE_JPEG",
                                    "IMAGE_JPG",
                                    "IMAGE_PNG"
                                ]
                            },
                            "FileInfo": {
                                "type": "object",
                                "required": [
                                    "fileIdentifier",
                                    "filename",
                                    "contentType",
                                    "contentLength",
                                    "updatedAt",
                                    "uploadedBy",
                                    "ownedBy",
                                    "metadata"
                                ],
                                "properties": {
                                    "fileIdentifier": {
                                        "type": "string"
                                    },
                                    "filename": {
                                        "type": "string"
                                    },
                                    "contentType": {
                                        "$ref": "#/components/schemas/ContentType"
                                    },
                                    "contentLength": {
                                        "type": "integer",
                                        "format": "int64"
                                    },
                                    "updatedAt": {
                                        "type": "string"
                                    },
                                    "uploadedBy": {
                                        "type": "string"
                                    },
                                    "ownedBy": {
                                        "type": "string"
                                    },
                                    "fileMetadata": {
                                        "type": "object",
                                        "additionalProperties": {
                                            "type": "array",
                                            "items": {
                                                "type": "string"
                                            }
                                        }
                                    },
                                    "customMetadata": {
                                        "type": "object",
                                        "additionalProperties": {
                                            "type": "string"
                                        }
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
                            "Traceability": {
                                "type": "object",
                                "required": [
                                    "token",
                                    "fileIdentifier",
                                    "downloadedBy",
                                    "downloadedAt"
                                ],
                                "properties": {
                                    "token": {
                                        "type": "string"
                                    },
                                    "fileIdentifier": {
                                        "type": "string"
                                    },
                                    "downloadedBy": {
                                        "type": "string"
                                    },
                                    "downloadedAt": {
                                        "type": "string"
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
                        "/exception/conflict": {
                            "post": {
                                "responses": {
                                    "409": {
                                        "description": "Conflict",
                                        "content": {
                                            "application/problem+json": {
                                                "schema": {
                                                    "$ref": "#/components/schemas/HttpProblem"
                                                }
                                            }
                                        }
                                    }
                                },
                                "summary": "Conflict",
                                "tags": [
                                    "Exception Resource"
                                ]
                            }
                        },
                        "/exception/defaultExceptionOpenapi": {
                            "post": {
                                "responses": {
                                    "201": {
                                        "description": "Created"
                                    }
                                },
                                "summary": "Default Exception Openapi",
                                "tags": [
                                    "Exception Resource"
                                ]
                            }
                        },
                        "/exception/failFastConditionNotMet": {
                            "post": {
                                "responses": {
                                    "400": {
                                        "description": "Bad request",
                                        "content": {
                                            "application/problem+json": {
                                                "schema": {
                                                    "$ref": "#/components/schemas/HttpProblem"
                                                }
                                            }
                                        }
                                    }
                                },
                                "summary": "Fail Fast Condition Not Met",
                                "tags": [
                                    "Exception Resource"
                                ]
                            }
                        },
                        "/exception/forbidden": {
                            "post": {
                                "responses": {
                                    "403": {
                                        "description": "Forbidden",
                                        "content": {
                                            "application/problem+json": {
                                                "schema": {
                                                    "$ref": "#/components/schemas/HttpProblem"
                                                }
                                            }
                                        }
                                    }
                                },
                                "summary": "Forbidden",
                                "tags": [
                                    "Exception Resource"
                                ]
                            }
                        },
                        "/exception/infrastructureFailure": {
                            "post": {
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
                                "summary": "Infrastructure Failure",
                                "tags": [
                                    "Exception Resource"
                                ]
                            }
                        },
                        "/exception/unknow": {
                            "post": {
                                "responses": {
                                    "404": {
                                        "description": "Not found",
                                        "content": {
                                            "application/problem+json": {
                                                "schema": {
                                                    "$ref": "#/components/schemas/HttpProblem"
                                                }
                                            }
                                        }
                                    }
                                },
                                "summary": "Unknow",
                                "tags": [
                                    "Exception Resource"
                                ]
                            }
                        },
                        "/file/{fileIdentifier}/download": {
                            "get": {
                                "summary": "Download a file",
                                "description": "Download a file identified by his identifier.",
                                "parameters": [
                                    {
                                        "description": "File identifier",
                                        "required": true,
                                        "schema": {
                                            "type": "string"
                                        },
                                        "name": "fileIdentifier",
                                        "in": "path"
                                    },
                                    {
                                        "description": "Content disposition",
                                        "required": false,
                                        "schema": {
                                            "type": "string",
                                            "enum": [
                                                "INLINE",
                                                "ATTACHMENT"
                                            ],
                                            "default": "INLINE"
                                        },
                                        "name": "contentDisposition",
                                        "in": "query"
                                    }
                                ],
                                "responses": {
                                    "200": {
                                        "description": "File downloaded successfully",
                                        "headers": {
                                            "Content-Disposition": {
                                                "description": "Content disposition",
                                                "schema": {
                                                    "type": "string"
                                                }
                                            },
                                            "Content-Type": {
                                                "description": "File MIME Type.",
                                                "schema": {
                                                    "type": "string"
                                                }
                                            },
                                            "Content-Length": {
                                                "description": "File length.",
                                                "schema": {
                                                    "type": "integer",
                                                    "format": "int64"
                                                }
                                            }
                                        },
                                        "content": {
                                            "*/*": {
                                                "schema": {
                                                    "type": "string",
                                                    "format": "binary"
                                                }
                                            }
                                        }
                                    },
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
                                "tags": [
                                    "File Endpoint"
                                ]
                            }
                        },
                        "/file/{fileIdentifier}/info": {
                            "get": {
                                "summary": "Get file info",
                                "description": "Get file info by his identifier.",
                                "parameters": [
                                    {
                                        "description": "File identifier",
                                        "required": true,
                                        "schema": {
                                            "type": "string"
                                        },
                                        "name": "fileIdentifier",
                                        "in": "path"
                                    }
                                ],
                                "responses": {
                                    "200": {
                                        "description": "File information retrieved successfully",
                                        "content": {
                                            "application/json": {
                                                "schema": {
                                                    "$ref": "#/components/schemas/FileInfo"
                                                }
                                            }
                                        }
                                    },
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
                                "tags": [
                                    "File Endpoint"
                                ]
                            }
                        },
                        "/file/{fileIdentifier}/traceByFileIdentifier": {
                            "get": {
                                "summary": "List of Traceability",
                                "description": "Return list of Traceability by his identifier.",
                                "parameters": [
                                    {
                                        "description": "File identifier",
                                        "required": true,
                                        "schema": {
                                            "type": "string"
                                        },
                                        "name": "fileIdentifier",
                                        "in": "path"
                                    }
                                ],
                                "responses": {
                                    "200": {
                                        "description": "Traceability list retrieved successfully",
                                        "content": {
                                            "application/json": {
                                                "schema": {
                                                    "type": "array",
                                                    "items": {
                                                        "$ref": "#/components/schemas/Traceability"
                                                    }
                                                }
                                            }
                                        }
                                    },
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
