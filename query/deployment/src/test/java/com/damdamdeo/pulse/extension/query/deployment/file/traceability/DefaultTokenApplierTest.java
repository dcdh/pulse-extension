package com.damdamdeo.pulse.extension.query.deployment.file.traceability;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.traceability.*;
import com.damdamdeo.pulse.extension.query.deployment.file.Resource;
import com.damdamdeo.pulse.extension.query.deployment.file.TestResourceProvider;
import com.damdamdeo.pulse.extension.core.query.file.traceability.ContentTypeTokenApplier;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DefaultTokenApplierTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResourceProvider.class, Resource.class)
                    .addAsResource("facture.jpeg")
                    .addAsResource("facture.jpg")
                    .addAsResource("facture.pdf")
                    .addAsResource("facture.png"))
            .withConfigurationResource("application.properties");

    @ApplicationScoped
    @Priority(1)
    @Alternative
    static class StubTokenGenerator implements TokenGenerator {

        final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Token generate() {
            return new Token(new UUID(0, counter.getAndAdd(1)));
        }
    }

    @ApplicationScoped
    @Priority(1)
    @Alternative
    static class StubDownloadedAtProvider implements DownloadedAtProvider {

        final AtomicInteger counter = new AtomicInteger(40);

        @Override
        public DownloadedAt now() {
            return new DownloadedAt(ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12), LocalTime.of(13, 46, counter.getAndAdd(1)), ZoneOffset.UTC));
        }
    }

    @Inject
    TokenApplier tokenApplier;

    @Inject
    TokenApplierTestSpy tokenApplierTestSpy;

    @Inject
    DataSource dataSource;

    @BeforeEach
    @AfterEach
    void tearDown() {
        tokenApplierTestSpy.reset();
    }

    @ApplicationScoped
    static class TokenApplierTestSpy {

        private final List<String> called = new ArrayList<>();

        public void add(String value) {
            called.add(value);
        }

        public List<String> getCalled() {
            return called;
        }

        public void reset() {
            called.clear();
        }
    }

    @Unremovable
    @Priority(1)
    @Decorator
    static class ContentTypeTokenApplierInterceptor implements ContentTypeTokenApplier {

        @Inject
        @Any
        @Delegate
        ContentTypeTokenApplier delegate;

        @Inject
        TokenApplierTestSpy tokenApplierTestSpy;

        @Override
        public FileContent apply(final FileContent fileContent, final Token token) throws UnableToApplyTokenException {
            tokenApplierTestSpy.add(String.join("|",
                    "apply",
                    fileContent.id().id(),
                    fileContent.contentType().contentType(),
                    token.value().toString()));
            return delegate.apply(fileContent, token);
        }

        @Override
        public List<ContentType> contentTypes() {
            return delegate.contentTypes();
        }
    }

    @Test
    @Order(1)
    void shouldApplyJpegToken() {
        // Given
        try {
            final Resource resource = TestResourceProvider.getResourceFromStream("/facture.jpeg");
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.jpeg"), ContentType.IMAGE_JPEG, resource.contentLength(), resource.payload());

            // When
            final FileContent applied = tokenApplier.apply(fileContent, Todo.OWNED_BY_USER_1);

            // Then
            assertAll(
                    () -> assertThat(applied.id()).isEqualTo(new FileIdentifier("facture.jpeg")),
                    () -> assertThat(applied.contentType()).isEqualTo(ContentType.IMAGE_JPEG),
                    () -> assertThat(applied.contentLength().contentLength()).isGreaterThan(1L),
                    () -> assertThat(tokenApplierTestSpy.getCalled()).containsExactly("apply|facture.jpeg|image/jpeg|00000000-0000-0000-0000-000000000000")
            );
        } catch (final TokenApplierException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    @Order(2)
    void shouldApplyJpgToken() {
        // Given
        try {
            final Resource resource = TestResourceProvider.getResourceFromStream("/facture.jpg");
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.jpg"), ContentType.IMAGE_JPG, resource.contentLength(), resource.payload());

            // When
            final FileContent applied = tokenApplier.apply(fileContent, Todo.OWNED_BY_USER_1);

            // Then
            assertAll(
                    () -> assertThat(applied.id()).isEqualTo(new FileIdentifier("facture.jpg")),
                    () -> assertThat(applied.contentType()).isEqualTo(ContentType.IMAGE_JPG),
                    () -> assertThat(applied.contentLength().contentLength()).isGreaterThan(1L),
                    () -> assertThat(tokenApplierTestSpy.getCalled()).containsExactly("apply|facture.jpg|image/jpg|00000000-0000-0000-0000-000000000001")
            );
        } catch (final TokenApplierException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    @Order(3)
    void shouldApplyPdfToken() {
        // Given
        try {
            final Resource resource = TestResourceProvider.getResourceFromStream("/facture.pdf");
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.pdf"), ContentType.APPLICATION_PDF, resource.contentLength(), resource.payload());

            // When
            final FileContent applied = tokenApplier.apply(fileContent, Todo.OWNED_BY_USER_1);

            // Then
            assertAll(
                    () -> assertThat(applied.id()).isEqualTo(new FileIdentifier("facture.pdf")),
                    () -> assertThat(applied.contentType()).isEqualTo(ContentType.APPLICATION_PDF),
                    () -> assertThat(applied.contentLength().contentLength()).isGreaterThan(1L),
                    () -> assertThat(tokenApplierTestSpy.getCalled()).containsExactly("apply|facture.pdf|application/pdf|00000000-0000-0000-0000-000000000002")
            );
        } catch (final TokenApplierException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    @Order(4)
    void shouldApplyPngToken() {
        // Given
        try {
            final Resource resource = TestResourceProvider.getResourceFromStream("/facture.png");
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.png"), ContentType.IMAGE_PNG, resource.contentLength(), resource.payload());

            // When
            final FileContent applied = tokenApplier.apply(fileContent, Todo.OWNED_BY_USER_1);

            // Then
            assertAll(
                    () -> assertThat(applied.id()).isEqualTo(new FileIdentifier("facture.png")),
                    () -> assertThat(applied.contentType()).isEqualTo(ContentType.IMAGE_PNG),
                    () -> assertThat(applied.contentLength().contentLength()).isGreaterThan(1L),
                    () -> assertThat(tokenApplierTestSpy.getCalled()).containsExactly("apply|facture.png|image/png|00000000-0000-0000-0000-000000000003")
            );
        } catch (final TokenApplierException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    @Order(5)
    void shouldStore() {
        final List<String> expected = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("SELECT token, file_identifier, downloaded_by, downloaded_at FROM pulse.token_download ORDER BY downloaded_at ASC")) {
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    expected.add(resultSet.getString("token") + "|" + resultSet.getString("file_identifier") + "|" + resultSet.getString("downloaded_by") + "|" + resultSet.getObject("downloaded_at", OffsetDateTime.class));
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        assertThat(expected).containsExactly("00000000-0000-0000-0000-000000000000|facture.jpeg|NA|1970-01-12T13:46:40Z",
                "00000000-0000-0000-0000-000000000001|facture.jpg|NA|1970-01-12T13:46:41Z",
                "00000000-0000-0000-0000-000000000002|facture.pdf|NA|1970-01-12T13:46:42Z",
                "00000000-0000-0000-0000-000000000003|facture.png|NA|1970-01-12T13:46:43Z");
    }
}
