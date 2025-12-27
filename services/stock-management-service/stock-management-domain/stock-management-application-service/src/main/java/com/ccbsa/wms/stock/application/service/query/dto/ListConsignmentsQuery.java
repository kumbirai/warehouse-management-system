package com.ccbsa.wms.stock.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;

/**
 * Query DTO: ListConsignmentsQuery
 * <p>
 * Query object for listing consignments with pagination.
 */
public final class ListConsignmentsQuery {
    private final TenantId tenantId;
    private final Integer page;
    private final Integer size;

    private ListConsignmentsQuery(Builder builder) {
        this.tenantId = builder.tenantId;
        this.page = builder.page;
        this.size = builder.size;
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

    public static class Builder {
        private TenantId tenantId;
        private Integer page;
        private Integer size;

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

        public ListConsignmentsQuery build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new ListConsignmentsQuery(this);
        }
    }
}

