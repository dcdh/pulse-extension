package com.damdamdeo.pulse.extension.common.deployment.featureflag;

import com.damdamdeo.pulse.extension.common.runtime.featureflag.ArcFeaturesProvider;
import com.damdamdeo.pulse.extension.core.featureflag.Feature;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;

class FeatureFlagTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @ApplicationScoped
    static class CustomEnableFeature implements Feature {

        @Override
        public String name() {
            return "CUSTOM_ENABLE_FEATURE";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }

    @ApplicationScoped
    static class CustomDisableFeature implements Feature {

        @Override
        public String name() {
            return "CUSTOM_DISABLE_FEATURE";
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    @Inject
    ArcFeaturesProvider arcFeaturesProvider;

    @Test
    void shouldReturnAllFeatures() {
        // Given

        // When
        final List<String> features = arcFeaturesProvider.provideAll().stream().map(Feature::name).toList();

        // Then
        assertThat(features).containsExactlyInAnyOrder("CUSTOM_DISABLE_FEATURE", "CUSTOM_ENABLE_FEATURE");
    }

    @Test
    void shouldReturnEnabledFeatures() {
        // Given

        // When / Then
        given()
                .when()
                .log().all()
                .get("/features")
                .then()
                .log().all()
                .statusCode(200)
                .body("$", contains("CUSTOM_ENABLE_FEATURE"));
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
                        "/features": {
                            "get": {
                                "summary": "Enabled features",
                                "description": "Returns enabled features.",
                                "responses": {
                                    "200": {
                                        "description": "Enabled features list retrieved successfully",
                                        "content": {
                                            "application/json": {
                                                "schema": {
                                                    "type": "array",
                                                    "examples": [
                                                        [
                                                            "TRACEABILITY"
                                                        ]
                                                    ],
                                                    "items": {
                                                        "type": "string"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                "tags": [
                                    "Feature Endpoint"
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
