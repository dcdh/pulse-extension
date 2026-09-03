package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.file.*;
import com.damdamdeo.pulse.extension.core.query.file.query.*;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DownloadedAt;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DownloadedBy;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Traceability;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tika.TikaMetadata;
import io.quarkus.tika.TikaParser;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileQueryTest {

    static final Logger LOGGER = Logger.getLogger(FileQueryTest.class.getName());

    private final static FileIdentifier GIVEN_FILE_IDENTIFIER = new FileIdentifier("F000001");

    private final static String EXPECTED_MD5 = "3b9124aacd88a0d0c4a1fbfeb0bc188c";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResourceProvider.class, Resource.class,
                            StubUploadedAtProvider.class, FileQuery.class, TokenDownload.class, ImageMD5.class)
                    .addAsResource("facture.jpg"))
            .withConfigurationResource("application.properties")
            .overrideRuntimeConfigKey("quarkus.cache.caffeine.\"file\".expire-after-write", "5s")
            // should trigger the schedule task
            .overrideRuntimeConfigKey("pulse.query.file.cleanup.every", "5s");

    @Inject
    FileQuery fileQuery;

    @Inject
    UploadQuery uploadQuery;

    @Inject
    DownloadQuery downloadQuery;

    @Inject
    GetFileInfoQuery getFileInfoQuery;

    @Inject
    GetTraceByFileIdentifierQuery getTraceByFileIdentifierQuery;

    @Inject
    TikaParser tikaParser;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    DataSource dataSource;

    @ApplicationScoped
    @Priority(1)
    @Alternative
    static class StubExecutionContextProvider implements ExecutionContextProvider {

        @Override
        public ExecutionContext provide() {
            return new ExecutionContext(
                    new ExecutedBy.EndUser(new Username("bob@mail.com")),
                    Set.of("user", "ADMIN"));
        }
    }

    @Test
    @Order(1)
    void shouldUploadFile() {
        // Given
        final FileIdentifier executed;

        // When
        try {
            final Resource resource = TestResourceProvider.getResourceFromStream("/facture.jpg");
            executed = uploadQuery.execute(new InputFile(
                    GIVEN_FILE_IDENTIFIER,
                    resource.contentLength(),
                    resource.payload(),
                    new Filename("facture.jpg"),
                    OwnedBy.from(GIVEN_FILE_IDENTIFIER),
                    new CustomMetadata(Map.of("key", "value"))
            ));
        } catch (final QueryException exception) {
            throw new RuntimeException(exception);
        }

        // Then
        assertThat(executed).isEqualTo(GIVEN_FILE_IDENTIFIER);
    }

    @Test
    @Order(2)
    void shouldDownloadFile() throws QueryException {
        // Given
        final DownloadInput givenDownloadInput = new DownloadInput(GIVEN_FILE_IDENTIFIER);

        // When
        final FileContent executed = downloadQuery.execute(givenDownloadInput);

        // Then
        final String token = fileQuery.retrieveLastToken(GIVEN_FILE_IDENTIFIER).token();
        final TikaMetadata parsed;
        final byte[] contentMd5;
        try (final InputStream payload = executed.content();
             final ByteArrayInputStream reusable = new ByteArrayInputStream(payload.readAllBytes())) {
            contentMd5 = ImageMD5.computeWithoutMetadata(reusable);
            reusable.reset();
            Files.copy(reusable,
                    Path.of("/tmp/%s.jpg".formatted(GIVEN_FILE_IDENTIFIER.id())),
                    StandardCopyOption.REPLACE_EXISTING);
            reusable.reset();
            parsed = tikaParser.getMetadata(reusable, "image/jpg");
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        assertAll(
                () -> assertThat(HexFormat.of().formatHex(contentMd5)).isEqualTo(EXPECTED_MD5),
                () -> assertThat(executed.id()).isEqualTo(GIVEN_FILE_IDENTIFIER),
                () -> assertThat(parsed.getValues("Exif IFD0:Windows XP Comment")).containsExactly(token)
        );
    }

    @Test
    @Order(3)
    void shouldGetFileInfo() throws QueryException {
        // Given

        // When
        final FileInfo executed = getFileInfoQuery.execute(GIVEN_FILE_IDENTIFIER);

        // Then
        assertAll(
                () -> assertThat(executed.fileIdentifier()).isEqualTo(GIVEN_FILE_IDENTIFIER),
                () -> assertThat(executed.fileMetadata().metadata()).isNotEmpty(),
                () -> assertThat(executed.contentLength().contentLength()).isEqualTo(287759L),
                () -> assertThat(executed.customMetadata().metadata()).isEqualTo(Map.of("key", "value"))
        );
    }

    @Test
    @Order(4)
    void shouldListTraceByFileIdentifier() throws QueryException {
        // Given

        // When
        final List<Traceability> executed = getTraceByFileIdentifierQuery.execute(GIVEN_FILE_IDENTIFIER);

        // Then
        final TokenDownload token = fileQuery.retrieveLastToken(GIVEN_FILE_IDENTIFIER);
        final DateTimeFormatter downloadedAtFormatter =
                new DateTimeFormatterBuilder()
                        .appendPattern("yyyy-MM-dd HH:mm:ss")
                        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                        .appendOffset("+HH", "Z")
                        .toFormatter();
        assertThat(executed).containsExactly(
                new Traceability(
                        new Token(UUID.fromString(token.token())),
                        GIVEN_FILE_IDENTIFIER,
                        new DownloadedBy(new ExecutedBy.EndUser(new Username("bob@mail.com"))),
                        new DownloadedAt(ZonedDateTime.parse(token.downloadedAt(), downloadedAtFormatter))
                )
        );
    }

    @Test
    @Order(5)
    void shouldUploadQueryInParallel() throws QueryException {
        // Given
        final int nbOfFiles = 10;
        final List<Runnable> uploads = new ArrayList<>();
        for (int i = 0; i < nbOfFiles; i++) {
            final int current = i;
            uploads.add(() -> {
                try {
                    final FileIdentifier fileIdentifier = new FileIdentifier("F" + current);
                    final Resource resource = TestResourceProvider.getResourceFromStream("/facture.jpg");
                    uploadQuery.execute(new InputFile(
                            fileIdentifier,
                            resource.contentLength(),
                            resource.payload(),
                            new Filename("facture.jpg"),
                            OwnedBy.from(fileIdentifier),
                            new CustomMetadata(Map.of("key", "value"))
                    ));
                } catch (final QueryException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // When - upload in parallel
        final List<CompletableFuture<Void>> uploadFutures = uploads.stream()
                .map(upload -> CompletableFuture.runAsync(upload, managedExecutor))
                .toList();

        CompletableFuture.allOf(uploadFutures.toArray(CompletableFuture[]::new)).join();

        // Then - download in parallel
        final List<CompletableFuture<byte[]>> downloadFutures =
                IntStream.range(0, nbOfFiles)
                        .mapToObj(current -> CompletableFuture.supplyAsync(() -> {
                            try {
                                final FileIdentifier fileIdentifier = new FileIdentifier("F" + current);
                                final FileContent executed = downloadQuery.execute(new DownloadInput(fileIdentifier));
                                try (final InputStream payload = executed.content();
                                     final ByteArrayInputStream reusable = new ByteArrayInputStream(payload.readAllBytes())) {
                                    Files.copy(reusable,
                                            Path.of("/tmp/%s.jpg".formatted(fileIdentifier.id())),
                                            StandardCopyOption.REPLACE_EXISTING);
                                    reusable.reset();
                                    return ImageMD5.computeWithoutMetadata(reusable);
                                }
                            } catch (final QueryException | IOException exception) {
                                throw new CompletionException(exception);
                            }
                        }, managedExecutor))
                        .toList();

        final List<byte[]> md5 = downloadFutures.stream()
                .map(CompletableFuture::join)
                .toList();

        assertThat(md5)
                .hasSize(nbOfFiles)
                .allSatisfy(contentMd5 -> assertThat(HexFormat.of().formatHex(contentMd5))
                        .isEqualTo(EXPECTED_MD5));

        final FileInfo executed = getFileInfoQuery.execute(new FileIdentifier("F0"));
        assertThat(executed.contentLength().contentLength()).isEqualTo(287759L);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT pg_size_pretty(pg_table_size('pulse.file')) AS table_size,
                                     pg_size_pretty(pg_indexes_size('pulse.file')) AS indexes_size,
                                     pg_size_pretty(pg_total_relation_size('pulse.file')) AS total_size
                             """);
             final ResultSet resultSet = preparedStatement.executeQuery()) {
            resultSet.next();
            LOGGER.info("pulse.file - table_size '%s' - indexes_size '%s' - total_size '%s'"
                    .formatted(resultSet.getString("table_size"),
                            resultSet.getString("indexes_size"),
                            resultSet.getString("total_size")));
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
