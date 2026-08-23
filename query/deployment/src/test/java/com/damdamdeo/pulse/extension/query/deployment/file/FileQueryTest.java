package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.FileInfo;
import com.damdamdeo.pulse.extension.core.query.file.Filename;
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
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileQueryTest {

    private final static FileIdentifier GIVEN_FILE_IDENTIFIER = new FileIdentifier("F000001");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResourceProvider.class, Resource.class,
                            StubUploadedAtProvider.class)
                    .addAsResource("facture.jpg"))
            .withConfigurationResource("application.properties")
            .overrideRuntimeConfigKey("quarkus.cache.caffeine.\"file\".expire-after-write", "5s")
            // should trigger the schedule task
            .overrideRuntimeConfigKey("pulse.query.file.cleanup.every", "5s");

    @Inject
    DataSource dataSource;

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

    @ApplicationScoped
    @Priority(1)
    @Alternative
    static class StubExecutionContextProvider implements ExecutionContextProvider {

        @Override
        public ExecutionContext provide() {
            return new ExecutionContext(
                    new ExecutedBy.EndUser("bob", true),
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

        // When
        final FileContent executed = downloadQuery.execute(new DownloadInput(GIVEN_FILE_IDENTIFIER));

        // Then
        final String token = retrieveLastToken().token();
        final TikaMetadata parsed = tikaParser.getMetadata(executed.content(), "image/jpg");

        assertAll(
                () -> assertThat(executed.id()).isEqualTo(GIVEN_FILE_IDENTIFIER),
                () -> assertThat(parsed.getValues("Exif IFD0:Windows XP Comment")).containsExactly(token)
        );
    }

    record TokenDownload(String token, String fileIdentifier, String downloadedBy, String downloadedAt) {

        TokenDownload {
            Objects.requireNonNull(token);
            Objects.requireNonNull(fileIdentifier);
            Objects.requireNonNull(downloadedBy);
            Objects.requireNonNull(downloadedAt);
        }
    }

    private TokenDownload retrieveLastToken() {
        final AtomicReference<TokenDownload> token = new AtomicReference<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("SELECT token, file_identifier, downloaded_by, downloaded_at FROM pulse.token_download WHERE file_identifier = ?")) {
            statement.setString(1, GIVEN_FILE_IDENTIFIER.id());
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    token.set(
                            new TokenDownload(resultSet.getString("token"),
                                    resultSet.getString("file_identifier"),
                                    resultSet.getString("downloaded_by"),
                                    resultSet.getString("downloaded_at")));
                }
            }
            Validate.validState(token.get() != null);
            return token.get();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
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
        final TokenDownload token = retrieveLastToken();
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
                        new DownloadedBy(token.downloadedBy()),
                        new DownloadedAt(ZonedDateTime.parse(token.downloadedAt(), downloadedAtFormatter))
                )
        );
    }
}
