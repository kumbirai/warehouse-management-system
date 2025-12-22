package com.ccbsa.wms.stock.application.service.command.dto;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentId;
import com.ccbsa.wms.stock.domain.core.valueobject.Quantity;

/**
 * Command DTO: CreateStockItemCommand
 * <p>
 * Command object for creating a new stock item.
 */
public final class CreateStockItemCommand {
    private final TenantId tenantId;
    private final ProductId productId;
    private final Quantity quantity;
    private final ExpirationDate expirationDate; // May be null for non-perishable
    private final LocationId locationId; // May be null initially
    private final ConsignmentId consignmentId; // Reference to source consignment

    private CreateStockItemCommand(Builder builder) {
        this.tenantId = builder.tenantId;
        this.productId = builder.productId;
        this.quantity = builder.quantity;
        this.expirationDate = builder.expirationDate;
        this.locationId = builder.locationId;
        this.consignmentId = builder.consignmentId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public ConsignmentId getConsignmentId() {
        return consignmentId;
    }

    public static class Builder {
        private TenantId tenantId;
        private ProductId productId;
        private Quantity quantity;
        private ExpirationDate expirationDate;
        private LocationId locationId;
        private ConsignmentId consignmentId;

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder productId(ProductId productId) {
            this.productId = productId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder expirationDate(ExpirationDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder consignmentId(ConsignmentId consignmentId) {
            this.consignmentId = consignmentId;
            return this;
        }

        public CreateStockItemCommand build() {
            if (tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (quantity == null) {
                throw new IllegalArgumentException("Quantity is required");
            }
            return new CreateStockItemCommand(this);
        }
    }
}

