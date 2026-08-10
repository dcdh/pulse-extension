package com.damdamdeo.pulse.extension.query.deployment.file.filigrane;

import com.damdamdeo.pulse.extension.core.query.file.*;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.UnableToApplyFiligraneException;
import com.damdamdeo.pulse.extension.query.runtime.file.filigrane.JpgAwtImageContentTypeFiligraneApplier;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JpgAwtImageContentTypeFiligraneApplierTest {

    private static final Logger LOGGER = Logger.getLogger(JpgAwtImageContentTypeFiligraneApplierTest.class);

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addAsResource("facture.jpg"))
            .withConfigurationResource("application.properties");

    @Inject
    JpgAwtImageContentTypeFiligraneApplier jpgAwtImageContentTypeFiligraneApplier;

    @Test
    void shouldApplyFiligrane() {
        // Given
        try (final InputStream inputStream = this.getClass().getResourceAsStream("/facture.jpg")) {
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.jpg"), ContentType.IMAGE_JPG, new ContentLength(1L), inputStream);

            // When
            final FileContent loremIpsumDolorSitAmet = jpgAwtImageContentTypeFiligraneApplier.apply(fileContent, "Lorem ipsum dolor sit amet");

            // Then
            final Path outputDirectory = Path.of("src", "test", "resources");

            final Path outputFile = outputDirectory.resolve("lorem-ipsum.jpg");
            try (final InputStream in = loremIpsumDolorSitAmet.content()) {
                Files.copy(in, outputFile, StandardCopyOption.REPLACE_EXISTING);
            }
            assertAll(
                    () -> assertThat(loremIpsumDolorSitAmet.id()).isEqualTo(new FileIdentifier("facture.jpg")),
                    () -> assertThat(loremIpsumDolorSitAmet.contentType()).isEqualTo(ContentType.IMAGE_JPG),
                    () -> assertThat(loremIpsumDolorSitAmet.contentLength().contentLength()).isGreaterThan(1L)
            );
            LOGGER.info("Please do a visual check on the generated file: %s".formatted("lorem-ipsum.jpg"));
        } catch (final IOException | UnableToApplyFiligraneException exception) {
            throw new RuntimeException(exception);
        }
    }
}
