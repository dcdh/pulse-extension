package com.damdamdeo.pulse.extension.common.runtime.encryption;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class FutureAwareInputStream extends FilterInputStream {

    private final CompletableFuture<?> completion;

    public FutureAwareInputStream(final InputStream delegate,
                                  final CompletableFuture<?> completion) {
        super(delegate);
        this.completion = Objects.requireNonNull(completion);
    }

    @Override
    public int read() throws IOException {
        final int value = super.read();
        if (value == -1) {
            awaitCompletion();
        }
        return value;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int value = super.read(b, off, len);
        if (value == -1) {
            awaitCompletion();
        }
        return value;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            awaitCompletion();
        }
    }

    private void awaitCompletion() throws IOException {
        try {
            completion.join();
        } catch (CompletionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IOException(cause);
        }
    }
}
