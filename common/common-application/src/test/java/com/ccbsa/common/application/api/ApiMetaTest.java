package com.ccbsa.common.application.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiMeta Tests")
class ApiMetaTest {
    @Test
    @DisplayName("Should create ApiMeta with pagination")
    void shouldCreateApiMetaWithPagination() {
        // Given
        ApiMeta.Pagination pagination = ApiMeta.Pagination.of(1, 20, 100);

        // When
        ApiMeta meta = ApiMeta.builder()
                .pagination(pagination)
                .build();

        // Then
        assertThat(meta).isNotNull();
        assertThat(meta.getPagination()).isEqualTo(pagination);
    }

    @Test
    @DisplayName("Should create ApiMeta without pagination")
    void shouldCreateApiMetaWithoutPagination() {
        // When
        ApiMeta meta = ApiMeta.builder()
                .build();

        // Then
        assertThat(meta).isNotNull();
        assertThat(meta.getPagination()).isNull();
    }

    @Test
    @DisplayName("Should create pagination with correct calculations")
    void shouldCreatePaginationWithCorrectCalculations() {
        // Given
        int page = 2;
        int size = 20;
        long totalElements = 100;

        // When
        ApiMeta.Pagination pagination = ApiMeta.Pagination.of(page, size, totalElements);

        // Then
        assertThat(pagination).isNotNull();
        assertThat(pagination.getPage()).isEqualTo(page);
        assertThat(pagination.getSize()).isEqualTo(size);
        assertThat(pagination.getTotalElements()).isEqualTo(totalElements);
        assertThat(pagination.getTotalPages()).isEqualTo(5); // 100 / 20 = 5
        assertThat(pagination.isHasNext()).isTrue();
        assertThat(pagination.isHasPrevious()).isTrue();
    }

    @Test
    @DisplayName("Should calculate pagination correctly for first page")
    void shouldCalculatePaginationCorrectlyForFirstPage() {
        // Given
        int page = 1;
        int size = 20;
        long totalElements = 100;

        // When
        ApiMeta.Pagination pagination = ApiMeta.Pagination.of(page, size, totalElements);

        // Then
        assertThat(pagination.getPage()).isEqualTo(1);
        assertThat(pagination.isHasPrevious()).isFalse();
        assertThat(pagination.isHasNext()).isTrue();
    }

    @Test
    @DisplayName("Should calculate pagination correctly for last page")
    void shouldCalculatePaginationCorrectlyForLastPage() {
        // Given
        int page = 5;
        int size = 20;
        long totalElements = 100;

        // When
        ApiMeta.Pagination pagination = ApiMeta.Pagination.of(page, size, totalElements);

        // Then
        assertThat(pagination.getPage()).isEqualTo(5);
        assertThat(pagination.isHasPrevious()).isTrue();
        assertThat(pagination.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("Should calculate pagination correctly for exact page boundary")
    void shouldCalculatePaginationCorrectlyForExactPageBoundary() {
        // Given
        int page = 1;
        int size = 20;
        long totalElements = 20; // Exactly one page

        // When
        ApiMeta.Pagination pagination = ApiMeta.Pagination.of(page, size, totalElements);

        // Then
        assertThat(pagination.getTotalPages()).isEqualTo(1);
        assertThat(pagination.isHasPrevious()).isFalse();
        assertThat(pagination.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("Should calculate pagination correctly for empty result")
    void shouldCalculatePaginationCorrectlyForEmptyResult() {
        // Given
        int page = 1;
        int size = 20;
        long totalElements = 0;

        // When
        ApiMeta.Pagination pagination = ApiMeta.Pagination.of(page, size, totalElements);

        // Then
        assertThat(pagination.getTotalPages()).isEqualTo(0);
        assertThat(pagination.isHasPrevious()).isFalse();
        assertThat(pagination.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("Should calculate pagination correctly with remainder")
    void shouldCalculatePaginationCorrectlyWithRemainder() {
        // Given
        int page = 1;
        int size = 20;
        long totalElements = 95; // 95 / 20 = 4.75, should round up to 5 pages

        // When
        ApiMeta.Pagination pagination = ApiMeta.Pagination.of(page, size, totalElements);

        // Then
        assertThat(pagination.getTotalPages()).isEqualTo(5); // Ceiling of 4.75
        assertThat(pagination.isHasNext()).isTrue();
    }
}

