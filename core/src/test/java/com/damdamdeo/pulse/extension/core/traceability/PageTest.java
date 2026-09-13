package com.damdamdeo.pulse.extension.core.traceability;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageTest {

    private static final List<String> CONTENT = List.of("bob", "alice");

    @Test
    void shouldCalculateTotalPages() {
        // Given
        final Pagination pagination = new Pagination(0, 2);
        final Page<String> page = new Page<>(CONTENT, pagination, 5);

        // When
        final int totalPages = page.totalPages();

        // Then
        assertEquals(3, totalPages);
    }

    @Test
    void shouldReturnOneTotalPageWhenTotalElementsIsLessThanPageSize() {
        // Given
        final Pagination pagination = new Pagination(0, 2);
        final Page<String> page = new Page<>(CONTENT, pagination, 1);

        // When
        final int totalPages = page.totalPages();

        // Then
        assertEquals(1, totalPages);
    }

    @Test
    void shouldReturnZeroTotalPagesWhenThereAreNoElements() {
        // Given
        final Pagination pagination = new Pagination(0, 2);
        final Page<String> page = new Page<>(List.of(), pagination, 0);

        // When
        final int totalPages = page.totalPages();

        // Then
        assertEquals(0, totalPages);
    }

    @Test
    void shouldHaveNextWhenCurrentPageIsNotLastPage() {
        // Given
        final Pagination pagination = new Pagination(0, 2);
        final Page<String> page = new Page<>(CONTENT, pagination, 5);

        // When
        final boolean hasNext = page.hasNext();

        // Then
        assertTrue(hasNext);
    }

    @Test
    void shouldNotHaveNextWhenCurrentPageIsLastPage() {
        // Given
        final Pagination pagination = new Pagination(2, 2);
        final Page<String> page = new Page<>(CONTENT, pagination, 5);

        // When
        final boolean hasNext = page.hasNext();

        // Then
        assertFalse(hasNext);
    }

    @Test
    void shouldHavePreviousWhenCurrentPageIsNotFirstPage() {
        // Given
        final Pagination pagination = new Pagination(1, 2);
        final Page<String> page = new Page<>(CONTENT, pagination, 5);

        // When
        final boolean hasPrevious = page.hasPrevious();

        // Then
        assertTrue(hasPrevious);
    }

    @Test
    void shouldNotHavePreviousWhenCurrentPageIsFirstPage() {
        // Given
        final Pagination pagination = new Pagination(0, 2);
        final Page<String> page = new Page<>(CONTENT, pagination, 5);

        // When
        final boolean hasPrevious = page.hasPrevious();

        // Then
        assertFalse(hasPrevious);
    }
}
