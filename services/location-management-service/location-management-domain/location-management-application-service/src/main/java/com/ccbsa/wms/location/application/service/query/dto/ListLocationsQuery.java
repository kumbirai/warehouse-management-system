package com.ccbsa.wms.location.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Query DTO: ListLocationsQuery
 * <p>
 * Query object for listing locations with optional filters.
 */
public final class ListLocationsQuery {
    private final TenantId tenantId;
    private final Integer page;
    private final Integer size;
    private final String zone;
    private final String status;
    private final String search;

    private ListLocationsQuery(Builder builder) {
        this.tenantId = builder.tenantId;
        this.page = builder.page;
        this.size = builder.size;
        this.zone = builder.zone;
        this.status = builder.status;
        this.search = builder.search;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }

    public String getZone() {
        return zone;
    }

    public String getStatus() {
        return status;
    }

    public String getSearch() {
        return search;
    }

    public static class Builder {
        private TenantId tenantId;
        private Integer page;
        private Integer size;
        private String zone;
        private String status;
        private String search;

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        public Builder zone(String zone) {
            this.zone = zone;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder search(String search) {
            this.search = search;
            return this;
        }

        public ListLocationsQuery build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new ListLocationsQuery(this);
        }
    }
}

