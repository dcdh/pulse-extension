package com.damdamdeo.pulse.extension.query.runtime.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.ContentLength;
import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.traceability.ContentTypeTokenApplier;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import com.damdamdeo.pulse.extension.core.query.file.traceability.UnableToApplyTokenException;
import com.damdamdeo.pulse.extension.query.runtime.file.filigrane.DeletingFileInputStream;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.Validate;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class PngContentTypeTokenApplier implements ContentTypeTokenApplier {

    @Override
    public FileContent apply(final FileContent fileContent, final Token token) throws UnableToApplyTokenException {
        Objects.requireNonNull(fileContent);
        Objects.requireNonNull(token);
        Path input = null;
        Path output = null;
        try {
            // ImageIO works on the stream, but the image itself is loaded in memory.
            // Provide a temporary file as input.
            final String extension = fileContent.contentType().extension();
            Validate.validState("png".equals(extension));
            input = Files.createTempFile("pulse-token-input-", "." + extension);
            try (final InputStream in = fileContent.content()) {
                Files.copy(in, input, StandardCopyOption.REPLACE_EXISTING);
            }
            final BufferedImage image = ImageIO.read(input.toFile());
            if (image == null) {
                throw new UnableToApplyTokenException(new IOException("Unable to read PNG image: " + input));
            }
            final ImageWriter writer = ImageIO.getImageWritersByFormatName(extension).next();
            try {
                final ImageWriteParam param = writer.getDefaultWriteParam();
                final ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
                final IIOMetadata metadata = writer.getDefaultImageMetadata(type, param);
                final String format = metadata.getNativeMetadataFormatName();
                final IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
                final IIOMetadataNode text = new IIOMetadataNode("tEXt");
                final IIOMetadataNode entry = new IIOMetadataNode("tEXtEntry");
                entry.setAttribute("keyword", "UserComment");
                entry.setAttribute("value", token.value().toString());
                text.appendChild(entry);
                root.appendChild(text);
                metadata.setFromTree(format, root);
                output = Files.createTempFile("pulse-token-output-", "." + extension);
                try (final ImageOutputStream ios = ImageIO.createImageOutputStream(output.toFile())) {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(image, null, metadata), param);
                }
            } finally {
                writer.dispose();
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
        } catch (final IOException | RuntimeException exception) {
            throw new UnableToApplyTokenException(exception);
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

    private void deleteQuietly(final Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (final IOException ignored) {
            }
        }
    }

    @Override
    public List<ContentType> contentTypes() {
        return List.of(ContentType.IMAGE_PNG);
    }
}
