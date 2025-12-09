package com.ccbsa.wms.tenant.application.service.query.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Query result DTO carrying tenants with pagination metadata.
 */
public final class TenantListResult {
    private final List<TenantView> tenants;
    private final int page;
    private final int size;
    private final long totalElements;

    public TenantListResult(List<TenantView> tenants,
                            int page,
                            int size,
                            long totalElements) {
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        this.tenants = tenants == null ? Collections.emptyList() : List.copyOf(tenants);
        this.page = page;
        this.size = size;
        this.totalElements = Math.max(totalElements,
                0L);
    }

    public List<TenantView> getTenants() {
        return Collections.unmodifiableList(new ArrayList<>(tenants));
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

    public boolean hasNext() {
        int totalPages = getTotalPages();
        return totalPages > 0 && page < totalPages;
    }

    public int getTotalPages() {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasPrevious() {
        return page > 1;
    }
}

