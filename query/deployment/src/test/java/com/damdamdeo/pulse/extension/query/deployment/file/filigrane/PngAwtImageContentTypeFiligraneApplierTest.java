package com.damdamdeo.pulse.extension.query.deployment.file.filigrane;

import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.UnableToApplyFiligraneException;
import com.damdamdeo.pulse.extension.query.deployment.file.Resource;
import com.damdamdeo.pulse.extension.query.deployment.file.TestResourceProvider;
import com.damdamdeo.pulse.extension.query.runtime.file.filigrane.PngAwtImageContentTypeFiligraneApplier;
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

class PngAwtImageContentTypeFiligraneApplierTest {

    private static final Logger LOGGER = Logger.getLogger(PngAwtImageContentTypeFiligraneApplierTest.class);

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResourceProvider.class, Resource.class)
                    .addAsResource("facture.png"))
            .withConfigurationResource("application.properties");

    @Inject
    PngAwtImageContentTypeFiligraneApplier pngAwtImageContentTypeFiligraneApplier;

    @Test
    void shouldApplyFiligrane() {
        // Given
        try {
            final Resource resource = TestResourceProvider.getResourceAsEncryptedStream("/facture.png");
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.png"), ContentType.IMAGE_PNG, resource.contentLength(), resource.payload());

            // When
            final FileContent loremIpsumDolorSitAmet = pngAwtImageContentTypeFiligraneApplier.apply(fileContent, "Lorem ipsum dolor sit amet");

            // Then
            final Path outputDirectory = Path.of("src", "test", "resources");

            final Path outputFile = outputDirectory.resolve("lorem-ipsum.png");
            try (final InputStream in = loremIpsumDolorSitAmet.content()) {
                Files.copy(in, outputFile, StandardCopyOption.REPLACE_EXISTING);
            }
            assertAll(
                    () -> assertThat(loremIpsumDolorSitAmet.id()).isEqualTo(new FileIdentifier("facture.png")),
                    () -> assertThat(loremIpsumDolorSitAmet.contentType()).isEqualTo(ContentType.IMAGE_PNG),
                    () -> assertThat(loremIpsumDolorSitAmet.contentLength().contentLength()).isGreaterThan(1L)
            );
            LOGGER.info("Please do a visual check on the generated file: %s".formatted("lorem-ipsum.png"));
        } catch (final IOException | UnableToApplyFiligraneException exception) {
            throw new RuntimeException(exception);
        }
    }
}
