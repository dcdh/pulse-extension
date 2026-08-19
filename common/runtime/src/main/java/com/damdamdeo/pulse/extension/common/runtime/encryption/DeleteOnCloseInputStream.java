package com.damdamdeo.pulse.extension.common.runtime.encryption;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class DeleteOnCloseInputStream extends FilterInputStream {

    private final Path file;

    public DeleteOnCloseInputStream(final InputStream delegate, final Path file) {
        super(delegate);
        this.file = Objects.requireNonNull(file);
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        try {
            super.close();
        } catch (final IOException e) {
            failure = e;
        }
        try {
            Files.deleteIfExists(file);
        } catch (final IOException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
