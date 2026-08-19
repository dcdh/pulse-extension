package com.damdamdeo.pulse.extension.query.deployment.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import com.damdamdeo.pulse.extension.core.query.file.traceability.UnableToApplyTokenException;
import com.damdamdeo.pulse.extension.query.deployment.file.Resource;
import com.damdamdeo.pulse.extension.query.deployment.file.TestResourceProvider;
import com.damdamdeo.pulse.extension.query.runtime.file.traceability.PngContentTypeTokenApplier;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tika.TikaContent;
import io.quarkus.tika.TikaParser;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PngContentTypeTokenApplierTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResourceProvider.class, Resource.class)
                    .addAsResource("facture.png"))
            .withConfigurationResource("application.properties");

    @Inject
    PngContentTypeTokenApplier pngContentTypeTokenApplier;

    @Inject
    TikaParser tikaParser;

    @Test
    void shouldApplyToken() {
        // Given
        try {
            final Resource resource = TestResourceProvider.getResourceFromStream("/facture.png");
            final FileContent fileContent = new FileContent(new FileIdentifier("facture.png"), ContentType.IMAGE_PNG, resource.contentLength(), resource.payload());

            // When
            final FileContent tokenized = pngContentTypeTokenApplier.apply(fileContent, new Token(new UUID(0, 0)));

            // Then
            final TikaContent parsed = tikaParser.parse(tokenized.content());

            assertAll(
                    () -> assertThat(tokenized.id()).isEqualTo(new FileIdentifier("facture.png")),
                    () -> assertThat(tokenized.contentType()).isEqualTo(ContentType.IMAGE_PNG),
                    () -> assertThat(tokenized.contentLength().contentLength()).isGreaterThan(1L),
                    () -> assertThat(parsed.getMetadata().getValues("Text TextEntry"))
                            .containsExactly("keyword=UserComment, value=00000000-0000-0000-0000-000000000000, encoding=ISO-8859-1, compression=none"),
                    () -> assertThat(parsed.getMetadata().getValues("tEXt tEXtEntry"))
                            .containsExactly("keyword=UserComment, value=00000000-0000-0000-0000-000000000000")
            );
        } catch (final UnableToApplyTokenException exception) {
            throw new RuntimeException(exception);
        }
    }
}
