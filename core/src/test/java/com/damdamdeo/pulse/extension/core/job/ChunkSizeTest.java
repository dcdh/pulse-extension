package com.damdamdeo.pulse.extension.core.job;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkSizeTest {

    @Test
    void shouldCreateAChunkSizeForAllItems() {
        // Given

        // When
        final ChunkSize chunkSize = new ChunkSize(-1);

        // Then
        assertAll(
                () -> assertEquals(-1, chunkSize.size()),
                () -> assertEquals(chunkSize, ChunkSize.ALL)
        );
    }

    @Test
    void shouldCreateAnEmptyChunkSize() {
        // Given

        // When
        final ChunkSize chunkSize = new ChunkSize(0);

        // Then
        assertEquals(0, chunkSize.size());
    }

    @Test
    void shouldCreateAPositiveChunkSize() {
        // Given

        // When
        final ChunkSize chunkSize = new ChunkSize(100);

        // Then
        assertEquals(100, chunkSize.size());
    }

    @Test
    void shouldRejectAChunkSizeLowerThanMinusOne() {
        // Given

        // When
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ChunkSize(-2)
        );

        // Then
        assertEquals("size must be greater than or equal to -1", exception.getMessage());
    }
}
