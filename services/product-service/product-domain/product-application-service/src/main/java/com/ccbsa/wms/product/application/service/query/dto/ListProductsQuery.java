package com.ccbsa.wms.product.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Query DTO: ListProductsQuery
 * <p>
 * Query object for listing products with optional filters.
 */
public final class ListProductsQuery {
    private final TenantId tenantId;
    private final Integer page;
    private final Integer size;
    private final String category;
    private final String brand;
    private final String search;

    private ListProductsQuery(Builder builder) {
        this.tenantId = builder.tenantId;
        this.page = builder.page;
        this.size = builder.size;
        this.category = builder.category;
        this.brand = builder.brand;
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

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public String getSearch() {
        return search;
    }

    public static class Builder {
        private TenantId tenantId;
        private Integer page;
        private Integer size;
        private String category;
        private String brand;
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

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder search(String search) {
            this.search = search;
            return this;
        }

        public ListProductsQuery build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new ListProductsQuery(this);
        }
    }
}

