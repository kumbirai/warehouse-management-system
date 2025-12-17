package com.ccbsa.wms.tenant.application.service.query.dto;

import java.util.Objects;
import java.util.Optional;

import com.ccbsa.wms.tenant.domain.core.valueobject.TenantStatus;

/**
 * Query DTO encapsulating pagination, filtering, and search parameters for tenant listing.
 */
public final class TenantListQuery {
    private static final int MIN_PAGE = 1;
    private static final int MIN_SIZE = 1;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final int page;
    private final int size;
    private final TenantStatus status;
    private final String search;

    private TenantListQuery(int page, int size, TenantStatus status, String search) {
        this.page = page;
        this.size = size;
        this.status = status;
        this.search = search;
    }

    public static TenantListQuery of(Integer page, Integer size, TenantStatus status, String search) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        String normalizedSearch = normalizeSearch(search);

        return new TenantListQuery(normalizedPage, normalizedSize, status, normalizedSearch);
    }

    private static int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        return Math.max(page, MIN_PAGE);
    }

    private static int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        int sanitized = Math.max(size, MIN_SIZE);
        return Math.min(sanitized, MAX_SIZE);
    }

    private static String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public Optional<TenantStatus> getStatus() {
        return Optional.ofNullable(status);
    }

    public Optional<String> getSearch() {
        return Optional.ofNullable(search);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TenantListQuery that = (TenantListQuery) o;
        return page == that.page && size == that.size && status == that.status && Objects.equals(search, that.search);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size, status, search);
    }
}

