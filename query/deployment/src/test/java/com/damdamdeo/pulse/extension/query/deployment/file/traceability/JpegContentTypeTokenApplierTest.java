package com.damdamdeo.pulse.extension.query.deployment.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.ContentLength;
import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import com.damdamdeo.pulse.extension.core.query.file.traceability.UnableToApplyTokenException;
import com.damdamdeo.pulse.extension.query.runtime.file.traceability.JpegContentTypeTokenApplier;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tika.TikaMetadata;
import io.quarkus.tika.TikaParser;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JpegContentTypeTokenApplierTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addAsResource("facture.jpg")
                    .addAsResource("facture.jpeg"))
            .withConfigurationResource("application.properties");

    @Inject
    JpegContentTypeTokenApplier jpegContentTypeTokenApplier;

    @Inject
    TikaParser tikaParser;

    // QuarkusUnitTest + @ParameterizedTest + @MethodSource : not working
    @Test
    void shouldApplyTokenToJpg() {
        shouldApplyToken("/facture.jpg", new FileIdentifier("facture.jpg"), ContentType.IMAGE_JPG);
    }

    @Test
    void shouldApplyTokenToJpeg() {
        shouldApplyToken("/facture.jpeg", new FileIdentifier("facture.jpeg"), ContentType.IMAGE_JPEG);
    }

    void shouldApplyToken(final String resourceName, final FileIdentifier fileIdentifier, final ContentType contentType) {
        // Given
        try (final InputStream inputStream = this.getClass().getResourceAsStream(resourceName)) {
            final FileContent fileContent = new FileContent(fileIdentifier, contentType, new ContentLength(1L), inputStream);

            // When
            final FileContent tokenized = jpegContentTypeTokenApplier.apply(fileContent, new Token(new UUID(0, 0)));

            // Then
            final TikaMetadata parsed = tikaParser.getMetadata(tokenized.content(), contentType.contentType());

            assertAll(
                    () -> assertThat(tokenized.id()).isEqualTo(fileIdentifier),
                    () -> assertThat(tokenized.contentType()).isEqualTo(contentType),
                    () -> assertThat(tokenized.contentLength().contentLength()).isGreaterThan(1L),
                    () -> assertThat(parsed.getValues("Exif IFD0:Windows XP Comment"))
                            .containsExactly("00000000-0000-0000-0000-000000000000")
            );
        } catch (final IOException | UnableToApplyTokenException exception) {
            throw new RuntimeException(exception);
        }
    }
}
