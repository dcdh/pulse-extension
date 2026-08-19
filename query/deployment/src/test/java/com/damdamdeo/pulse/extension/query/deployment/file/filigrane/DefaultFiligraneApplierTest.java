package com.damdamdeo.pulse.extension.query.deployment.file.filigrane;

import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.UnableToApplyFiligraneException;
import com.damdamdeo.pulse.extension.query.deployment.file.Resource;
import com.damdamdeo.pulse.extension.query.deployment.file.TestResourceProvider;
import com.damdamdeo.pulse.extension.query.runtime.file.filigrane.ContentTypeFiligraneApplier;
import com.damdamdeo.pulse.extension.query.runtime.file.filigrane.DefaultFiligraneApplier;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class DefaultFiligraneApplierTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResourceProvider.class, Resource.class)
                    .addAsResource("facture.jpeg")
                    .addAsResource("facture.jpg")
                    .addAsResource("facture.pdf")
                    .addAsResource("facture.png"))
            .overrideConfigKey("pulse.query.file.filigrane", "lorem ipsum")
            .withConfigurationResource("application.properties");

    @Inject
    DefaultFiligraneApplier defaultFiligraneApplier;

    @Inject
    FiligraneTestSpy filigraneTestSpy;

    @BeforeEach
    @AfterEach
    void tearDown() {
        filigraneTestSpy.reset();
    }

    @ApplicationScoped
    static class FiligraneTestSpy {

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
    static class ContentTypeFiligraneApplierInterceptor implements ContentTypeFiligraneApplier {

        @Inject
        @Any
        @Delegate
        ContentTypeFiligraneApplier delegate;

        @Inject
        FiligraneTestSpy filigraneTestSpy;

        @Override
        public FileContent apply(final FileContent fileContent, final String text) throws UnableToApplyFiligraneException {
            filigraneTestSpy.add(String.join("|",
                    "apply",
                    fileContent.id().id(),
                    fileContent.contentType().contentType(),
                    text));
            return delegate.apply(fileContent, text);
        }

        @Override
        public ContentType contentType() {
            return delegate.contentType();
        }
    }

    @Test
    void shouldApplyJpegFiligrane() {
        // Given
        try {
            final Resource resource = TestResourceProvider.getResourceAsEncryptedStream("/facture.jpeg");
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.jpeg"), ContentType.IMAGE_JPEG, resource.contentLength(), resource.payload());

            // When
            final FileContent applied = defaultFiligraneApplier.apply(fileContent);

            // Then
            assertAll(
                    () -> assertThat(applied.id()).isEqualTo(new FileIdentifier("facture.jpeg")),
                    () -> assertThat(applied.contentType()).isEqualTo(ContentType.IMAGE_JPEG),
                    () -> assertThat(applied.contentLength().contentLength()).isGreaterThan(1L),
                    () -> assertThat(filigraneTestSpy.getCalled()).containsExactly("apply|facture.jpeg|image/jpeg|lorem ipsum")
            );
        } catch (final UnableToApplyFiligraneException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    void shouldApplyJpgFiligrane() {
        // Given
        try {
            final Resource resource = TestResourceProvider.getResourceAsEncryptedStream("/facture.jpg");
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.jpg"), ContentType.IMAGE_JPG, resource.contentLength(), resource.payload());

            // When
            final FileContent applied = defaultFiligraneApplier.apply(fileContent);

            // Then
            assertAll(
                    () -> assertThat(applied.id()).isEqualTo(new FileIdentifier("facture.jpg")),
                    () -> assertThat(applied.contentType()).isEqualTo(ContentType.IMAGE_JPG),
                    () -> assertThat(applied.contentLength().contentLength()).isGreaterThan(1L),
                    () -> assertThat(filigraneTestSpy.getCalled()).containsExactly("apply|facture.jpg|image/jpg|lorem ipsum")
            );
        } catch (final UnableToApplyFiligraneException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    void shouldApplyPdfFiligrane() {
        // Given
        try {
            final Resource resource = TestResourceProvider.getResourceAsEncryptedStream("/facture.pdf");
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.pdf"), ContentType.APPLICATION_PDF, resource.contentLength(), resource.payload());

            // When
            final FileContent applied = defaultFiligraneApplier.apply(fileContent);

            // Then
            assertAll(
                    () -> assertThat(applied.id()).isEqualTo(new FileIdentifier("facture.pdf")),
                    () -> assertThat(applied.contentType()).isEqualTo(ContentType.APPLICATION_PDF),
                    () -> assertThat(applied.contentLength().contentLength()).isGreaterThan(1L),
                    () -> assertThat(filigraneTestSpy.getCalled()).containsExactly("apply|facture.pdf|application/pdf|lorem ipsum")
            );
        } catch (final UnableToApplyFiligraneException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    void shouldApplyPngFiligrane() {
        // Given
        try {
            final Resource resource = TestResourceProvider.getResourceAsEncryptedStream("/facture.png");
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.png"), ContentType.IMAGE_PNG, resource.contentLength(), resource.payload());

            // When
            final FileContent applied = defaultFiligraneApplier.apply(fileContent);

            // Then
            assertAll(
                    () -> assertThat(applied.id()).isEqualTo(new FileIdentifier("facture.png")),
                    () -> assertThat(applied.contentType()).isEqualTo(ContentType.IMAGE_PNG),
                    () -> assertThat(applied.contentLength().contentLength()).isGreaterThan(1L),
                    () -> assertThat(filigraneTestSpy.getCalled()).containsExactly("apply|facture.png|image/png|lorem ipsum")
            );
        } catch (final UnableToApplyFiligraneException exception) {
            throw new RuntimeException(exception);
        }
    }
}
