package com.damdamdeo.pulse.extension.query.runtime.file.filigrane;

import com.damdamdeo.pulse.extension.core.query.file.ContentLength;
import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.UnableToApplyFiligraneException;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class PdfBoxContentTypeFiligraneApplier implements ContentTypeFiligraneApplier {

    @Override
    public FileContent apply(final FileContent fileContent, final String text) throws UnableToApplyFiligraneException {
        Objects.requireNonNull(fileContent);
        Objects.requireNonNull(text);
        // Loader can take an input stream, but it will load it all in memory.
        // Provide a temporary file instead.
        Path inTempFile = null;
        try {
            inTempFile = Files.createTempFile("pulse-filigrane-input-", "." + contentType().extension());
            try (final InputStream in = fileContent.content()) {
                Files.copy(in, inTempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            try (final PDDocument document = Loader.loadPDF(inTempFile.toFile())) {
                for (final PDPage page : document.getPages()) {

                    final PDPageContentStream content = new PDPageContentStream(
                            document,
                            page,
                            PDPageContentStream.AppendMode.APPEND,
                            true,
                            true);

                    final PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                    gs.setNonStrokingAlphaConstant(0.2f);
                    content.setGraphicsStateParameters(gs);

                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 50);
                    content.setNonStrokingColor(Color.LIGHT_GRAY);
                    content.setTextMatrix(
                            Matrix.getRotateInstance(Math.toRadians(45), 200, 400));
                    content.showText(text);
                    content.endText();
                    content.close();
                }

                final Path tempFile = Files.createTempFile("pulse-filigrane-output-", "." + contentType().extension());
                try (final OutputStream out = Files.newOutputStream(tempFile)) {
                    document.save(out);
                }
                return new FileContent(
                        fileContent.id(),
                        fileContent.contentType(),
                        new ContentLength(Files.size(tempFile)),
                        DeletingFileInputStream.from(tempFile)
                );
            } catch (final IOException exception) {
                throw new UnableToApplyFiligraneException(exception);
            }
        } catch (final IOException exception) {
            throw new UnableToApplyFiligraneException(exception);
        } finally {
            if (inTempFile != null) {
                try {
                    Files.deleteIfExists(inTempFile);
                } catch (final IOException exception) {
                    // fail silently
                }
            }
        }
    }

    @Override
    public ContentType contentType() {
        return ContentType.APPLICATION_PDF;
    }
}
