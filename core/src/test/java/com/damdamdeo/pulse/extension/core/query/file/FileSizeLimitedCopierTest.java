package com.damdamdeo.pulse.extension.core.query.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileSizeLimitedCopierTest {

    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private final FileSizeLimitedCopier copier = new FileSizeLimitedCopier();

    @TempDir
    Path tempDir;

    @Test
    void shouldCopyFile() throws Exception {
        final Path source = tempDir.resolve("source.txt");
        final Path destination = tempDir.resolve("destination.txt");

        final byte[] content = "Hello World".getBytes();
        Files.write(source, content);

        try (final InputStream input = Files.newInputStream(source)) {
            copier.copy(input, destination, MAX_SIZE);
        }

        assertAll(
                () -> assertTrue(Files.exists(destination)),
                () -> assertArrayEquals(content, Files.readAllBytes(destination))
        );
    }

    @Test
    void shouldCopyFileWhenSizeIsExactlyMaxSize() throws Exception {
        final Path destination = tempDir.resolve("destination.txt");
        final byte[] content = new byte[1024];

        try (final InputStream input = new java.io.ByteArrayInputStream(content)) {
            copier.copy(input, destination, 1024L);
        }

        assertAll(
                () -> assertTrue(Files.exists(destination)),
                () -> assertEquals(1024L, Files.size(destination))
        );
    }

    @Test
    void shouldThrowWhenFileSizeExceedsMaxSize() {
        final Path destination = tempDir.resolve("destination.txt");
        final byte[] content = new byte[1025];

        final UnableToCopyException exception = assertThrows(
                UnableToCopyException.class,
                () -> {
                    try (final InputStream input =
                                 new java.io.ByteArrayInputStream(content)) {
                        copier.copy(input, destination, 1024L);
                    }
                }
        );
        assertInstanceOf(MaxFileSizeReachedException.class, exception.getCause());
    }

    @Test
    void shouldDeleteDestinationWhenFileSizeExceedsMaxSize() throws Exception {
        final Path destination = tempDir.resolve("destination.txt");
        Files.writeString(destination, "old content");
        final byte[] content = new byte[1025];

        assertThrows(
                UnableToCopyException.class,
                () -> {
                    try (final InputStream input =
                                 new java.io.ByteArrayInputStream(content)) {
                        copier.copy(input, destination, 1024L);
                    }
                }
        );
        assertFalse(Files.exists(destination));
    }

    @Test
    void shouldRejectZeroMaxSize() {
        final Path destination = tempDir.resolve("destination.txt");

        assertThrows(
                IllegalArgumentException.class,
                () -> copier.copy(
                        InputStream.nullInputStream(),
                        destination,
                        0L
                )
        );
    }

    @Test
    void shouldRejectNegativeMaxSize() {
        final Path destination = tempDir.resolve("destination.txt");

        assertThrows(
                IllegalArgumentException.class,
                () -> copier.copy(
                        InputStream.nullInputStream(),
                        destination,
                        -1L
                )
        );
    }

    @Test
    void shouldThrowUnableToCopyWhenInputCannotBeRead() throws Exception {
        final Path destination = tempDir.resolve("destination.txt");

        final InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Read error");
            }
        };

        final UnableToCopyException exception = assertThrows(
                UnableToCopyException.class,
                () -> copier.copy(input, destination, MAX_SIZE)
        );

        assertAll(
                () -> assertInstanceOf(IOException.class, exception.getCause()),
                () -> assertEquals("Read error", exception.getCause().getMessage())
        );
    }

    @Test
    void shouldDeleteDestinationWhenInputCannotBeRead() throws Exception {
        final Path destination = tempDir.resolve("destination.txt");

        final InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Read error");
            }
        };

        assertThrows(
                UnableToCopyException.class,
                () -> copier.copy(input, destination, MAX_SIZE)
        );

        assertFalse(Files.exists(destination));
    }
}
