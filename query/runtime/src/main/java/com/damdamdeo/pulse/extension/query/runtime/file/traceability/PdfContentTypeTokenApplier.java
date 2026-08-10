package com.damdamdeo.pulse.extension.query.runtime.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.ContentLength;
import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import com.damdamdeo.pulse.extension.core.query.file.traceability.UnableToApplyTokenException;
import com.damdamdeo.pulse.extension.query.runtime.file.filigrane.DeletingFileInputStream;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.Validate;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class PdfContentTypeTokenApplier implements ContentTypeTokenApplier {

    @Override
    public FileContent apply(final FileContent fileContent, final Token token) throws UnableToApplyTokenException {
        Objects.requireNonNull(fileContent);
        Objects.requireNonNull(token);
        // Loader can take an input stream, but it will load it all in memory.
        // Provide a temporary file instead.
        Path input = null;
        Path output = null;
        try {
            final String extension = fileContent.contentType().extension();
            Validate.validState("pdf".equals(extension));
            input = Files.createTempFile("pulse-token-input-", "." + extension);
            output = Files.createTempFile("pulse-token-output-", "." + extension);
            try (final InputStream in = fileContent.content()) {
                Files.copy(in, input, StandardCopyOption.REPLACE_EXISTING);
            }
            try (final PDDocument document = Loader.loadPDF(input.toFile())) {
                final PDDocumentInformation info = document.getDocumentInformation();
                info.setCustomMetadataValue("UserComment", token.value().toString());
                document.setDocumentInformation(info);
                document.save(output.toFile());
            }
            final long contentLength = Files.size(output);
            final InputStream content = DeletingFileInputStream.from(output);
            // Ownership of the output file is transferred to the InputStream.
            output = null;
            return new FileContent(
                    fileContent.id(),
                    fileContent.contentType(),
                    new ContentLength(contentLength),
                    content
            );
        } catch (final IOException exception) {
            throw new UnableToApplyTokenException(exception);
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

    private static void deleteQuietly(final Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (final IOException ignored) {
            // Fail silently.
        }
    }

    @Override
    public List<ContentType> contentTypes() {
        return List.of(ContentType.APPLICATION_PDF);
    }
}
