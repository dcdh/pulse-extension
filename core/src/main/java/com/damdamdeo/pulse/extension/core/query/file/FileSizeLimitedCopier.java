package com.damdamdeo.pulse.extension.core.query.file;

import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class FileSizeLimitedCopier {

    // TODO copy should return the size of the copied file
    public void copy(final InputStream input, final Path destination, final Long maxSize) throws UnableToCopyException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(destination);
        Objects.requireNonNull(maxSize);
        Validate.isTrue(maxSize > 0, "maxSize must be greater than 0");
        try (final OutputStream output = Files.newOutputStream(
                destination,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            final byte[] buffer = new byte[8192];
            long total = 0;

            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxSize) {
                    throw new MaxFileSizeReachedException();
                }
                output.write(buffer, 0, read);
            }
        } catch (final IOException | MaxFileSizeReachedException exception) {
            try {
                Files.deleteIfExists(destination);
                throw new UnableToCopyException(exception);
            } catch (final IOException e) {
                throw new UnableToCopyException(exception);
            }
        }
    }
}
