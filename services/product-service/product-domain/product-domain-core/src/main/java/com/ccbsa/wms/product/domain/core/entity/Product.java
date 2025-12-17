package com.ccbsa.wms.product.domain.core.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.domain.core.event.ProductCreatedEvent;
import com.ccbsa.wms.product.domain.core.event.ProductUpdatedEvent;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;
import com.ccbsa.wms.product.domain.core.valueobject.UnitOfMeasure;

/**
 * Aggregate Root: Product
 * <p>
 * Represents a product in the warehouse management system.
 * <p>
 * Business Rules: - Products are tenant-aware - Each product has a unique product code per tenant - Each product has a unique primary barcode per tenant - Products can have
 * multiple secondary barcodes - Product code cannot be changed after
 * creation - Primary barcode can be updated (must remain unique)
 */
public class Product
        extends TenantAwareAggregateRoot<ProductId> {

    // Value Objects
    private ProductCode productCode;
    private ProductBarcode primaryBarcode;
    private UnitOfMeasure unitOfMeasure;

    // Collections
    private List<ProductBarcode> secondaryBarcodes;

    // Primitives
    private String description;
    private String category;
    private String brand;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    /**
     * Private constructor for builder pattern. Prevents direct instantiation.
     */
    private Product() {
        this.secondaryBarcodes = new ArrayList<>();
    }

    /**
     * Factory method to create builder instance.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Business logic method: Updates the product description.
     * <p>
     * Business Rules: - Description cannot be null or empty - Description cannot exceed 500 characters
     *
     * @param newDescription New description value
     * @throws IllegalArgumentException if newDescription is invalid
     */
    public void updateDescription(String newDescription) {
        if (newDescription == null || newDescription.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (newDescription.length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 characters");
        }

        this.description = newDescription.trim();
        this.lastModifiedAt = LocalDateTime.now();

        // Publish update event
        addDomainEvent(
                new ProductUpdatedEvent(this.getId(), this.getTenantId(), this.productCode, this.description, this.primaryBarcode, this.secondaryBarcodes, this.unitOfMeasure,
                        this.category, this.brand));
    }

    /**
     * Business logic method: Updates the primary barcode.
     * <p>
     * Business Rules: - Primary barcode must be unique per tenant (validated at application service layer) - Primary barcode cannot be null - New primary barcode cannot be the
     * same as any secondary barcode
     *
     * @param newBarcode New primary barcode value
     * @throws IllegalArgumentException if newBarcode is null or conflicts with secondary barcodes
     */
    public void updatePrimaryBarcode(ProductBarcode newBarcode) {
        if (newBarcode == null) {
            throw new IllegalArgumentException("PrimaryBarcode cannot be null");
        }
        if (hasBarcode(newBarcode.getValue())) {
            throw new IllegalArgumentException(String.format("Primary barcode %s conflicts with existing secondary barcode", newBarcode.getValue()));
        }

        this.primaryBarcode = newBarcode;
        this.lastModifiedAt = LocalDateTime.now();

        // Publish update event
        addDomainEvent(
                new ProductUpdatedEvent(this.getId(), this.getTenantId(), this.productCode, this.description, this.primaryBarcode, this.secondaryBarcodes, this.unitOfMeasure,
                        this.category, this.brand));
    }

    /**
     * Query method: Checks if product has a specific barcode (primary or secondary).
     *
     * @param barcodeValue Barcode value to check
     * @return true if product has the barcode
     */
    public boolean hasBarcode(String barcodeValue) {
        if (barcodeValue == null || barcodeValue.trim()
                .isEmpty()) {
            return false;
        }

        // Check primary barcode
        if (primaryBarcode != null && primaryBarcode.getValue()
                .equals(barcodeValue.trim())) {
            return true;
        }

        // Check secondary barcodes
        return secondaryBarcodes.stream()
                .anyMatch(barcode -> barcode.getValue()
                        .equals(barcodeValue.trim()));
    }

    /**
     * Business logic method: Adds a secondary barcode.
     * <p>
     * Business Rules: - Secondary barcode must be unique per tenant (validated at application service layer) - Secondary barcode cannot be null - Secondary barcode cannot be the
     * same as primary barcode - Secondary barcode cannot be
     * duplicated in the list
     *
     * @param barcode Secondary barcode to add
     * @throws IllegalArgumentException if barcode is invalid or conflicts
     */
    public void addSecondaryBarcode(ProductBarcode barcode) {
        if (barcode == null) {
            throw new IllegalArgumentException("SecondaryBarcode cannot be null");
        }
        if (primaryBarcode != null && primaryBarcode.getValue()
                .equals(barcode.getValue())) {
            throw new IllegalArgumentException("Secondary barcode cannot be the same as primary barcode");
        }
        if (hasBarcode(barcode.getValue())) {
            throw new IllegalArgumentException(String.format("Secondary barcode %s already exists", barcode.getValue()));
        }

        this.secondaryBarcodes.add(barcode);
        this.lastModifiedAt = LocalDateTime.now();

        // Publish update event
        addDomainEvent(
                new ProductUpdatedEvent(this.getId(), this.getTenantId(), this.productCode, this.description, this.primaryBarcode, this.secondaryBarcodes, this.unitOfMeasure,
                        this.category, this.brand));
    }

    /**
     * Business logic method: Removes a secondary barcode.
     * <p>
     * Business Rules: - Barcode must exist in the secondary barcodes list
     *
     * @param barcode Secondary barcode to remove
     * @throws IllegalArgumentException if barcode is null or not found
     */
    public void removeSecondaryBarcode(ProductBarcode barcode) {
        if (barcode == null) {
            throw new IllegalArgumentException("SecondaryBarcode cannot be null");
        }
        if (!this.secondaryBarcodes.remove(barcode)) {
            throw new IllegalArgumentException(String.format("Secondary barcode %s not found", barcode.getValue()));
        }

        this.lastModifiedAt = LocalDateTime.now();

        // Publish update event
        addDomainEvent(
                new ProductUpdatedEvent(this.getId(), this.getTenantId(), this.productCode, this.description, this.primaryBarcode, this.secondaryBarcodes, this.unitOfMeasure,
                        this.category, this.brand));
    }

    /**
     * Business logic method: Updates the unit of measure.
     *
     * @param newUnitOfMeasure New unit of measure
     * @throws IllegalArgumentException if newUnitOfMeasure is null
     */
    public void updateUnitOfMeasure(UnitOfMeasure newUnitOfMeasure) {
        if (newUnitOfMeasure == null) {
            throw new IllegalArgumentException("UnitOfMeasure cannot be null");
        }

        this.unitOfMeasure = newUnitOfMeasure;
        this.lastModifiedAt = LocalDateTime.now();

        // Publish update event
        addDomainEvent(
                new ProductUpdatedEvent(this.getId(), this.getTenantId(), this.productCode, this.description, this.primaryBarcode, this.secondaryBarcodes, this.unitOfMeasure,
                        this.category, this.brand));
    }

    /**
     * Business logic method: Updates optional fields (category, brand).
     *
     * @param category New category (can be null)
     * @param brand    New brand (can be null)
     */
    public void updateOptionalFields(String category, String brand) {
        this.category = category != null ? category.trim() : null;
        this.brand = brand != null ? brand.trim() : null;
        this.lastModifiedAt = LocalDateTime.now();

        // Publish update event
        addDomainEvent(
                new ProductUpdatedEvent(this.getId(), this.getTenantId(), this.productCode, this.description, this.primaryBarcode, this.secondaryBarcodes, this.unitOfMeasure,
                        this.category, this.brand));
    }

    /**
     * Query method: Gets all barcodes (primary and secondary).
     *
     * @return List of all barcodes
     */
    public List<ProductBarcode> getAllBarcodes() {
        List<ProductBarcode> allBarcodes = new ArrayList<>();
        if (primaryBarcode != null) {
            allBarcodes.add(primaryBarcode);
        }
        allBarcodes.addAll(secondaryBarcodes);
        return Collections.unmodifiableList(allBarcodes);
    }

    // Getters (read-only access)

    public ProductCode getProductCode() {
        return productCode;
    }

    public ProductBarcode getPrimaryBarcode() {
        return primaryBarcode;
    }

    public List<ProductBarcode> getSecondaryBarcodes() {
        return Collections.unmodifiableList(secondaryBarcodes);
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Builder class for constructing Product instances. Ensures all required fields are set and validated.
     */
    public static class Builder {
        private Product product = new Product();

        public Builder productId(ProductId id) {
            product.setId(id);
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            product.setTenantId(tenantId);
            return this;
        }

        public Builder productCode(ProductCode productCode) {
            product.productCode = productCode;
            return this;
        }

        public Builder description(String description) {
            product.description = description;
            return this;
        }

        public Builder primaryBarcode(ProductBarcode primaryBarcode) {
            product.primaryBarcode = primaryBarcode;
            return this;
        }

        public Builder secondaryBarcode(ProductBarcode secondaryBarcode) {
            if (product.secondaryBarcodes == null) {
                product.secondaryBarcodes = new ArrayList<>();
            }
            product.secondaryBarcodes.add(secondaryBarcode);
            return this;
        }

        public Builder secondaryBarcodes(List<ProductBarcode> secondaryBarcodes) {
            if (product.secondaryBarcodes == null) {
                product.secondaryBarcodes = new ArrayList<>();
            }
            if (secondaryBarcodes != null) {
                product.secondaryBarcodes.addAll(secondaryBarcodes);
            }
            return this;
        }

        public Builder unitOfMeasure(UnitOfMeasure unitOfMeasure) {
            product.unitOfMeasure = unitOfMeasure;
            return this;
        }

        public Builder category(String category) {
            product.category = category;
            return this;
        }

        public Builder brand(String brand) {
            product.brand = brand;
            return this;
        }

        /**
         * Sets the creation timestamp (for loading from database).
         *
         * @param createdAt Creation timestamp
         * @return Builder instance
         */
        public Builder createdAt(LocalDateTime createdAt) {
            product.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the last modified timestamp (for loading from database).
         *
         * @param lastModifiedAt Last modified timestamp
         * @return Builder instance
         */
        public Builder lastModifiedAt(LocalDateTime lastModifiedAt) {
            product.lastModifiedAt = lastModifiedAt;
            return this;
        }

        /**
         * Sets the version (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(int version) {
            product.setVersion(version);
            return this;
        }

        /**
         * Sets the version as Long (for loading from database).
         *
         * @param version Version number
         * @return Builder instance
         */
        public Builder version(Long version) {
            product.setVersion(version != null ? version.intValue() : 0);
            return this;
        }

        /**
         * Builds and validates the Product instance.
         *
         * @return Validated Product instance
         * @throws IllegalArgumentException if validation fails
         */
        public Product build() {
            validate();
            initializeDefaults();

            // Set createdAt if not already set (for new products)
            if (product.createdAt == null) {
                product.createdAt = LocalDateTime.now();
            }
            if (product.lastModifiedAt == null) {
                product.lastModifiedAt = LocalDateTime.now();
            }

            // Publish creation event only if this is a new product (no version set)
            if (product.getVersion() == 0) {
                product.addDomainEvent(
                        new ProductCreatedEvent(product.getId(), product.getTenantId(), product.productCode, product.description, product.primaryBarcode, product.secondaryBarcodes,
                                product.unitOfMeasure, product.category, product.brand));
            }

            return consumeProduct();
        }

        /**
         * Validates all required fields are set.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            if (product.getId() == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (product.getTenantId() == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (product.productCode == null) {
                throw new IllegalArgumentException("ProductCode is required");
            }
            if (product.description == null || product.description.trim()
                    .isEmpty()) {
                throw new IllegalArgumentException("Description is required");
            }
            if (product.description.length() > 500) {
                throw new IllegalArgumentException("Description cannot exceed 500 characters");
            }
            if (product.primaryBarcode == null) {
                throw new IllegalArgumentException("PrimaryBarcode is required");
            }
            if (product.unitOfMeasure == null) {
                throw new IllegalArgumentException("UnitOfMeasure is required");
            }
        }

        /**
         * Initializes default values for optional fields.
         */
        private void initializeDefaults() {
            if (product.secondaryBarcodes == null) {
                product.secondaryBarcodes = new ArrayList<>();
            }
        }

        /**
         * Consumes the product from the builder and returns it. Creates a new product instance for the next build.
         *
         * @return Built product
         */
        private Product consumeProduct() {
            Product builtProduct = product;
            product = new Product();
            return builtProduct;
        }

        /**
         * Builds Product without publishing creation event. Used when reconstructing from persistence.
         *
         * @return Validated Product instance
         * @throws IllegalArgumentException if validation fails
         */
        public Product buildWithoutEvents() {
            validate();
            initializeDefaults();

            // Set createdAt if not already set
            if (product.createdAt == null) {
                product.createdAt = LocalDateTime.now();
            }
            if (product.lastModifiedAt == null) {
                product.lastModifiedAt = LocalDateTime.now();
            }

            // Do not publish events when loading from database
            return consumeProduct();
        }
    }
}

