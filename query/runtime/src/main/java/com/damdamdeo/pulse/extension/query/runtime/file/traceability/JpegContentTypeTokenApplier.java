package com.damdamdeo.pulse.extension.query.runtime.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.ContentLength;
import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import com.damdamdeo.pulse.extension.core.query.file.traceability.UnableToApplyTokenException;
import com.damdamdeo.pulse.extension.query.runtime.file.filigrane.DeletingFileInputStream;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.MicrosoftTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

// https://exiv2.org/tags.html
@ApplicationScoped
@Unremovable
public final class JpegContentTypeTokenApplier implements ContentTypeTokenApplier {

    @Override
    public FileContent apply(final FileContent fileContent, final Token token) throws UnableToApplyTokenException {
        Objects.requireNonNull(fileContent);
        Objects.requireNonNull(token);
        Path input = null;
        Path output = null;
        try {
            final String extension = fileContent.contentType().extension();
            Validate.validState("jpg".equals(extension) || "jpeg".equals(extension));
            input = Files.createTempFile("pulse-token-input-", "." + extension);
            try (final InputStream is = fileContent.content();
                 final OutputStream os = Files.newOutputStream(input)) {
                is.transferTo(os);
            }

            TiffOutputSet outputSet = null;
            // note that metadata might be null if no metadata is found.
            final ImageMetadata metadata = Imaging.getMetadata(input.toFile());
            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                // note that exif might be null if no Exif metadata is found.
                final TiffImageMetadata exif = jpegMetadata.getExif();
                if (null != exif) {
                    outputSet = exif.getOutputSet();
                }
            }
            // if file does not contain any exif metadata, we create an empty
            // set of exif metadata. Otherwise, we keep all the other
            // existing tags.
            if (null == outputSet) {
                outputSet = new TiffOutputSet();
            }

            final TiffOutputDirectory rootDir = outputSet.getOrCreateRootDirectory();
            rootDir.removeField(MicrosoftTagConstants.EXIF_TAG_XPCOMMENT);
            rootDir.add(MicrosoftTagConstants.EXIF_TAG_XPCOMMENT, token.value().toString());

            output = Files.createTempFile("pulse-token-output-", "." + extension);
            try (final InputStream is = Files.newInputStream(input);
                 final OutputStream os = Files.newOutputStream(output)) {
                new ExifRewriter().updateExifMetadataLossy(is, os, outputSet);
            }

            final long size = Files.size(output);
            final InputStream result = DeletingFileInputStream.from(output);
            // ownership transferred to DeletingFileInputStream
            output = null;
            return new FileContent(
                    fileContent.id(),
                    fileContent.contentType(),
                    new ContentLength(size),
                    result
            );
        } catch (final IOException exception) {
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
        return List.of(ContentType.IMAGE_JPEG, ContentType.IMAGE_JPG);
    }
}
