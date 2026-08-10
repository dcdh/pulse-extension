package com.damdamdeo.pulse.extension.query.runtime.file.filigrane;

import com.damdamdeo.pulse.extension.core.query.file.ContentLength;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.UnableToApplyFiligraneException;
import org.apache.commons.lang3.Validate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public abstract class AwtImageContentTypeFiligraneApplier implements ContentTypeFiligraneApplier {

    @Override
    public FileContent apply(final FileContent fileContent, final String text) throws UnableToApplyFiligraneException {
        Objects.requireNonNull(fileContent);
        Objects.requireNonNull(text);
        Validate.validState(fileContent.contentType().equals(contentType()));
        try {
            final BufferedImage image = ImageIO.read(fileContent.content());
            final Graphics2D g2d = image.createGraphics();

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.setColor(Color.GRAY);

            g2d.rotate(Math.toRadians(-45), image.getWidth() / 2.0, image.getHeight() / 2.0);

            g2d.drawString(text, image.getWidth() / 4, image.getHeight() / 2);
            g2d.dispose();

            final Path tempFile = Files.createTempFile("pulse-filigrane-output-", "." + contentType().extension());
            try (final OutputStream out = Files.newOutputStream(tempFile)) {
                ImageIO.write(image, contentType().extension(), out);
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
    }
}
