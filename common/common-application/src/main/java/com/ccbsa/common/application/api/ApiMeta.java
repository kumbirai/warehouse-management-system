package com.ccbsa.common.application.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Metadata for API responses (pagination, etc.).
 *
 * <p>Used for pagination and other metadata:</p>
 * <pre>{@code
 * {
 *   "meta": {
 *     "pagination": {
 *       "page": 1,
 *       "size": 20,
 *       "totalElements": 150,
 *       "totalPages": 8,
 *       "hasNext": true,
 *       "hasPrevious": false
 *     }
 *   }
 * }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiMeta {
    private Pagination pagination;

    private ApiMeta(Builder builder) {
        this.pagination = builder.pagination;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Pagination getPagination() {
        return pagination;
    }

    /**
     * Builder for ApiMeta.
     */
    public static final class Builder {
        private Pagination pagination;

        private Builder() {
        }

        public Builder pagination(Pagination pagination) {
            this.pagination = pagination;
            return this;
        }

        public ApiMeta build() {
            return new ApiMeta(this);
        }
    }

    /**
     * Pagination metadata.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Pagination {
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean hasNext;
        private final boolean hasPrevious;

        public Pagination(int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }

        public static Pagination of(int page, int size, long totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / size);
            boolean hasNext = page < totalPages;
            boolean hasPrevious = page > 1;
            return new Pagination(page, size, totalElements, totalPages, hasNext, hasPrevious);
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public boolean isHasNext() {
            return hasNext;
        }

        public boolean isHasPrevious() {
            return hasPrevious;
        }
    }
}

