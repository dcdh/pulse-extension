package com.damdamdeo.pulse.extension.query.runtime.file.filigrane;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class DeletingFileInputStream extends FilterInputStream {

    private final Path file;

    private DeletingFileInputStream(final Path file) throws IOException {
        super(Files.newInputStream(file));
        this.file = Objects.requireNonNull(file);
    }

    public static InputStream from(final Path file) throws IOException {
        return new DeletingFileInputStream(file);
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
