package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.Filename;
import com.damdamdeo.pulse.extension.core.query.file.CustomMetadata;
import com.damdamdeo.pulse.extension.core.query.file.query.InputFile;
import com.damdamdeo.pulse.extension.core.query.file.query.UploadQuery;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tika.TikaMetadata;
import io.quarkus.tika.TikaParser;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

/**
 * @see com.damdamdeo.pulse.extension.livenotifier.deployment.consumer.LiveConnectedConsumerTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileQueryEndpointTest {

    private final static FileIdentifier GIVEN_FILE_IDENTIFIER = new FileIdentifier("F000001");

    private static final DateTimeFormatter POSTGRES_DATE_TIME =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .appendPattern("x")
                    .toFormatter();

    static String accessToken = null;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResourceProvider.class, Resource.class,
                            StubUploadedAtProvider.class, FileQuery.class, TokenDownload.class)
                    .addAsResource("facture.jpg"))
            .withConfigurationResource("application.properties")
            .overrideRuntimeConfigKey("quarkus.cache.caffeine.\"file\".expire-after-write", "5s")
            // should trigger the schedule task
            .overrideRuntimeConfigKey("pulse.query.file.cleanup.every", "5s")
            .overrideConfigKey("quarkus.oidc.client-id", "account")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion())
            ));

    @Inject
    FileQuery fileQuery;

    @Inject
    TikaParser tikaParser;

    @Path("upload")
    static class UploadEndpoint {

        @Inject
        UploadQuery uploadQuery;

        @POST
        @Produces(MediaType.TEXT_PLAIN)
        public String upload() {
            try {
                final Resource resource = TestResourceProvider.getResourceFromStream("/facture.jpg");
                return uploadQuery.execute(new InputFile(
                        GIVEN_FILE_IDENTIFIER,
                        resource.contentLength(),
                        resource.payload(),
                        new Filename("facture.jpg"),
                        OwnedBy.from(GIVEN_FILE_IDENTIFIER),
                        new CustomMetadata(Map.of("key", "value"))
                )).id();
            } catch (final QueryException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    @Test
    @Order(1)
    void shouldUploadFile() {
        // Given
        final String authServerUrl = ConfigProvider.getConfig().getValue("quarkus.oidc.auth-server-url", String.class);
        final String clientId = ConfigProvider.getConfig().getValue("quarkus.oidc.client-id", String.class);
        final String secret = ConfigProvider.getConfig().getValue("quarkus.oidc.credentials.secret", String.class);
        accessToken =
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

        given()
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .when()
                .log().all()
                .post("/upload")
                .then().log().all()
                .statusCode(200)
                .body(is(GIVEN_FILE_IDENTIFIER.id()));
    }

    @Test
    @Order(2)
    void shouldDownloadFile() throws IOException {
        // Given
        Objects.requireNonNull(accessToken);
        final java.nio.file.Path outputFile = java.nio.file.Path.of("src", "test", "resources",
                "file-query-endpoint-generated-file.jpg");
        final String token;
        final TikaMetadata parsed;

        // When
        try (final InputStream inputStream = given()
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .when()
                .pathParam("fileIdentifier", GIVEN_FILE_IDENTIFIER.id())
                .log().all()
                .get("/file/{fileIdentifier}/download")
                .then()
                //.log().all() // file content
                .statusCode(200)
                .extract()
                .asInputStream()) {
            Files.copy(inputStream, outputFile, StandardCopyOption.REPLACE_EXISTING);
            parsed = tikaParser.getMetadata(Files.newInputStream(outputFile), "image/jpg");
            token = fileQuery.retrieveLastToken(GIVEN_FILE_IDENTIFIER).token();
        }

        // Then
        assertThat(parsed.getValues("Exif IFD0:Windows XP Comment")).containsExactly(token);
    }

    @Test
    @Order(3)
    void shouldGetFileInfo() {
        // Given
        Objects.requireNonNull(accessToken);

        // When / Then
        given()
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .when()
                .pathParam("fileIdentifier", GIVEN_FILE_IDENTIFIER.id())
                .log().all()
                .get("/file/{fileIdentifier}/info")
                .then().log().all()
                .statusCode(200)
                .body("fileIdentifier", equalTo("F000001"))
                .body("filename", equalTo("facture.jpg"))
                .body("contentType", equalTo("IMAGE_JPG"))
                .body("contentLength", equalTo(287759))
                .body("updatedAt", equalTo("2026-08-05T23:00:31Z"))
                .body("uploadedBy", equalTo("EU:bob@mail.com"))
                .body("ownedBy", equalTo("F000001"))
                .body("fileMetadata.size()", greaterThan(0))
                .body("customMetadata.size()", greaterThan(0));
    }

    @Test
    @Order(4)
    void shouldListTraceByFileIdentifier() {
        // Given
        Objects.requireNonNull(accessToken);
        final TokenDownload token = fileQuery.retrieveLastToken(GIVEN_FILE_IDENTIFIER);

        // When
        given()
                .header("Authorization", "Bearer %s".formatted(accessToken))
                .when()
                .pathParam("fileIdentifier", GIVEN_FILE_IDENTIFIER.id())
                .log().all()
                .get("/file/{fileIdentifier}/traceByFileIdentifier")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].token", is(token.token()))
                .body("[0].fileIdentifier", is(GIVEN_FILE_IDENTIFIER.id()))
                .body("[0].downloadedBy", is("EU:bob@mail.com"))
                .body("[0].downloadedAt", is(OffsetDateTime.parse(token.downloadedAt(), POSTGRES_DATE_TIME)
                        .toInstant().toString()
                ));
    }
}
