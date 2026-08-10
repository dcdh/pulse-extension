package com.damdamdeo.pulse.extension.query.deployment.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.ContentLength;
import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import com.damdamdeo.pulse.extension.core.query.file.traceability.UnableToApplyTokenException;
import com.damdamdeo.pulse.extension.query.runtime.file.traceability.PdfContentTypeTokenApplier;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tika.TikaContent;
import io.quarkus.tika.TikaParser;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PdfContentTypeTokenApplierTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addAsResource("facture.pdf"))
            .withConfigurationResource("application.properties");

    @Inject
    PdfContentTypeTokenApplier pdfContentTypeTokenApplier;

    @Inject
    TikaParser tikaParser;

    @Test
    void shouldApplyToken() {
        // Given
        try (final InputStream inputStream = this.getClass().getResourceAsStream("/facture.pdf")) {
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.pdf"), ContentType.APPLICATION_PDF, new ContentLength(1L), inputStream);

            // When
            final FileContent tokenized = pdfContentTypeTokenApplier.apply(fileContent, new Token(new UUID(0, 0)));

            // Then
            final TikaContent parsed = tikaParser.parse(tokenized.content());

            assertAll(
                    () -> assertThat(tokenized.id()).isEqualTo(new FileIdentifier("facture.pdf")),
                    () -> assertThat(tokenized.contentType()).isEqualTo(ContentType.APPLICATION_PDF),
                    () -> assertThat(tokenized.contentLength().contentLength()).isGreaterThan(1L),
                    () -> assertThat(parsed.getMetadata().getValues("UserComment"))
                            .containsExactly("00000000-0000-0000-0000-000000000000")
            );
        } catch (final IOException | UnableToApplyTokenException exception) {
            throw new RuntimeException(exception);
        }
    }
}
