package com.ccbsa.wms.stockmanagement.application.service.query.dto;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentId;

/**
 * Query DTO: GetConsignmentQuery
 * <p>
 * Query object for retrieving a consignment by ID.
 */
public final class GetConsignmentQuery {
    private final ConsignmentId consignmentId;
    private final TenantId tenantId;

    private GetConsignmentQuery(Builder builder) {
        this.consignmentId = builder.consignmentId;
        this.tenantId = builder.tenantId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ConsignmentId getConsignmentId() {
        return consignmentId;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public static class Builder {
        private ConsignmentId consignmentId;
        private TenantId tenantId;

        public Builder consignmentId(ConsignmentId consignmentId) {
            this.consignmentId = consignmentId;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public GetConsignmentQuery build() {
            if (consignmentId == null) {
                throw new IllegalArgumentException("ConsignmentId is required");
            }
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            return new GetConsignmentQuery(this);
        }
    }
}

