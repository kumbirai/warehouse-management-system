# Enforce Min/Max Stock Levels Implementation Plan

## US-5.1.2: Enforce Minimum and Maximum Stock Levels

**Service:** Stock Management Service
**Priority:** Must Have
**Story Points:** 5
**Sprint:** Sprint 4

---

## Overview

### User Story

**As a** warehouse manager
**I want** minimum and maximum stock levels enforced
**So that** stock is maintained within optimal thresholds

### Business Requirements

- System maintains minimum and maximum levels per product
- Levels may vary by location or warehouse
- System prevents stock levels from exceeding maximum
- System alerts when stock approaches minimum
- System validates thresholds when updating stock levels

### Technical Requirements

- Builds on US-5.1.1 Monitor Stock Levels
- Domain events for threshold violations
- Configuration per product/location
- Real-time validation
- Notification integration

---

## UI Design

### Min/Max Configuration Form

**Component:** `MinMaxStockLevelConfig.tsx`

```typescript
<Form onSubmit={handleSubmit}>
  <Grid container spacing={2}>
    <Grid item xs={12}>
      <ProductAutocomplete
        label="Product"
        value={product}
        onChange={setProduct}
        required
      />
    </Grid>
    <Grid item xs={12}>
      <LocationAutocomplete
        label="Location (Optional)"
        value={location}
        onChange={setLocation}
        helperText="Leave empty for warehouse-wide configuration"
      />
    </Grid>
    <Grid item xs={12} md={6}>
      <TextField
        label="Minimum Quantity"
        type="number"
        value={minimumQuantity}
        onChange={(e) => setMinimumQuantity(Number(e.target.value))}
        inputProps={{ min: 0 }}
        required
        helperText="Alert when stock falls below this level"
      />
    </Grid>
    <Grid item xs={12} md={6}>
      <TextField
        label="Maximum Quantity"
        type="number"
        value={maximumQuantity}
        onChange={(e) => setMaximumQuantity(Number(e.target.value))}
        inputProps={{ min: minimumQuantity }}
        required
        helperText="Prevent stock from exceeding this level"
      />
    </Grid>
    <Grid item xs={12}>
      <FormControlLabel
        control={
          <Checkbox
            checked={enableAutoRestock}
            onChange={(e) => setEnableAutoRestock(e.target.checked)}
          />
        }
        label="Enable automatic restock requests when below minimum"
      />
    </Grid>
    <Grid item xs={12}>
      <Button type="submit" variant="contained">
        Save Configuration
      </Button>
    </Grid>
  </Grid>
</Form>
```

### Stock Threshold Alerts

**Component:** `StockThresholdAlerts.tsx`

```typescript
<Alert severity={getAlertSeverity(violation.type)}>
  <AlertTitle>{violation.type}</AlertTitle>
  <Typography variant="body2">
    Product: {violation.productCode} - {violation.productName}
  </Typography>
  <Typography variant="body2">
    Current Level: {violation.currentQuantity} /
    Threshold: {violation.thresholdQuantity}
  </Typography>
  <Typography variant="body2">
    Location: {violation.locationCode || 'All Locations'}
  </Typography>
  <Button size="small" onClick={() => viewDetails(violation)}>
    View Details
  </Button>
</Alert>
```

---

## Domain Model Design

### StockLevelThreshold Aggregate Root

**Package:** `com.ccbsa.wms.stock.domain.core.entity`

```java
package com.ccbsa.wms.stock.domain.core.entity;

import com.ccbsa.common.domain.TenantAwareAggregateRoot;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.MinimumQuantity;
import com.ccbsa.common.domain.valueobject.MaximumQuantity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockLevelThresholdId;
import com.ccbsa.wms.stock.domain.core.event.StockLevelBelowMinimumEvent;
import com.ccbsa.wms.stock.domain.core.event.StockLevelAboveMaximumEvent;

import java.time.LocalDateTime;

/**
 * Aggregate Root: StockLevelThreshold
 * <p>
 * Maintains minimum and maximum stock level thresholds.
 * <p>
 * Business Rules:
 * - Minimum quantity must be less than maximum quantity
 * - Thresholds can be defined per product or per product/location
 * - One threshold configuration per product/location combination
 */
public class StockLevelThreshold extends TenantAwareAggregateRoot<StockLevelThresholdId> {

    private ProductId productId;
    private LocationId locationId;  // NULL for warehouse-wide threshold
    private MinimumQuantity minimumQuantity;
    private MaximumQuantity maximumQuantity;
    private boolean enableAutoRestock;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    private StockLevelThreshold() {
        // Private constructor for builder
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Business logic method: Update minimum quantity.
     */
    public void updateMinimumQuantity(MinimumQuantity newMinimum) {
        if (newMinimum.getValue() >= this.maximumQuantity.getValue()) {
            throw new IllegalArgumentException(
                "Minimum quantity must be less than maximum quantity"
            );
        }

        this.minimumQuantity = newMinimum;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Update maximum quantity.
     */
    public void updateMaximumQuantity(MaximumQuantity newMaximum) {
        if (newMaximum.getValue() <= this.minimumQuantity.getValue()) {
            throw new IllegalArgumentException(
                "Maximum quantity must be greater than minimum quantity"
            );
        }

        this.maximumQuantity = newMaximum;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Business logic method: Validate stock level against thresholds.
     */
    public void validateStockLevel(int currentQuantity) {
        if (currentQuantity < minimumQuantity.getValue()) {
            addDomainEvent(new StockLevelBelowMinimumEvent(
                this.getId(),
                this.tenantId,
                this.productId,
                this.locationId,
                currentQuantity,
                minimumQuantity.getValue(),
                this.enableAutoRestock
            ));
        }

        if (currentQuantity > maximumQuantity.getValue()) {
            addDomainEvent(new StockLevelAboveMaximumEvent(
                this.getId(),
                this.tenantId,
                this.productId,
                this.locationId,
                currentQuantity,
                maximumQuantity.getValue()
            ));
        }
    }

    /**
     * Business logic method: Check if quantity exceeds maximum.
     */
    public boolean wouldExceedMaximum(int quantityToAdd, int currentQuantity) {
        return (currentQuantity + quantityToAdd) > maximumQuantity.getValue();
    }

    /**
     * Business logic method: Check if quantity is below minimum.
     */
    public boolean isBelowMinimum(int currentQuantity) {
        return currentQuantity < minimumQuantity.getValue();
    }

    // Getters
    public ProductId getProductId() {
        return productId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public MinimumQuantity getMinimumQuantity() {
        return minimumQuantity;
    }

    public MaximumQuantity getMaximumQuantity() {
        return maximumQuantity;
    }

    public boolean isEnableAutoRestock() {
        return enableAutoRestock;
    }

    // Builder pattern
    public static class Builder {
        private StockLevelThreshold threshold = new StockLevelThreshold();

        public Builder stockLevelThresholdId(StockLevelThresholdId id) {
            threshold.id = id;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            threshold.tenantId = tenantId;
            return this;
        }

        public Builder productId(ProductId productId) {
            threshold.productId = productId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            threshold.locationId = locationId;
            return this;
        }

        public Builder minimumQuantity(MinimumQuantity minimumQuantity) {
            threshold.minimumQuantity = minimumQuantity;
            return this;
        }

        public Builder maximumQuantity(MaximumQuantity maximumQuantity) {
            threshold.maximumQuantity = maximumQuantity;
            return this;
        }

        public Builder enableAutoRestock(boolean enableAutoRestock) {
            threshold.enableAutoRestock = enableAutoRestock;
            return this;
        }

        public StockLevelThreshold build() {
            validate();
            threshold.createdAt = LocalDateTime.now();
            threshold.lastModifiedAt = LocalDateTime.now();
            return threshold;
        }

        private void validate() {
            if (threshold.id == null) {
                throw new IllegalArgumentException("StockLevelThresholdId is required");
            }
            if (threshold.tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (threshold.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (threshold.minimumQuantity == null) {
                throw new IllegalArgumentException("MinimumQuantity is required");
            }
            if (threshold.maximumQuantity == null) {
                throw new IllegalArgumentException("MaximumQuantity is required");
            }
            if (threshold.minimumQuantity.getValue() >= threshold.maximumQuantity.getValue()) {
                throw new IllegalArgumentException(
                    "Minimum quantity must be less than maximum quantity"
                );
            }
        }
    }
}
```

### MinimumQuantity Value Object (Move to common-domain)

**Package:** `com.ccbsa.common.domain.valueobject`

```java
package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: MinimumQuantity
 * <p>
 * Represents minimum stock quantity threshold.
 * Shared across services (DRY principle).
 */
public final class MinimumQuantity {
    private final int value;

    private MinimumQuantity(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Minimum quantity cannot be negative");
        }
        this.value = value;
    }

    public static MinimumQuantity of(int value) {
        return new MinimumQuantity(value);
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinimumQuantity that = (MinimumQuantity) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return "MinimumQuantity{" + value + "}";
    }
}
```

### MaximumQuantity Value Object (Move to common-domain)

**Package:** `com.ccbsa.common.domain.valueobject`

```java
package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: MaximumQuantity
 * <p>
 * Represents maximum stock quantity threshold.
 * Shared across services (DRY principle).
 */
public final class MaximumQuantity {
    private final int value;

    private MaximumQuantity(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Maximum quantity cannot be negative");
        }
        this.value = value;
    }

    public static MaximumQuantity of(int value) {
        return new MaximumQuantity(value);
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaximumQuantity that = (MaximumQuantity) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return "MaximumQuantity{" + value + "}";
    }
}
```

---

## Backend Implementation

### Command Handlers

```java
@Component
public class ConfigureStockLevelThresholdCommandHandler {

    private final StockLevelThresholdRepository repository;
    private final StockManagementEventPublisher eventPublisher;

    @Transactional
    public ConfigureStockLevelThresholdResult handle(
        ConfigureStockLevelThresholdCommand command
    ) {
        // 1. Check if threshold already exists
        Optional<StockLevelThreshold> existingThreshold =
            command.getLocationId() != null
                ? repository.findByTenantIdAndProductIdAndLocationId(
                    command.getTenantId(),
                    command.getProductId(),
                    command.getLocationId()
                  )
                : repository.findByTenantIdAndProductId(
                    command.getTenantId(),
                    command.getProductId()
                  );

        StockLevelThreshold threshold;
        if (existingThreshold.isPresent()) {
            // Update existing
            threshold = existingThreshold.get();
            threshold.updateMinimumQuantity(command.getMinimumQuantity());
            threshold.updateMaximumQuantity(command.getMaximumQuantity());
        } else {
            // Create new
            threshold = StockLevelThreshold.builder()
                .stockLevelThresholdId(StockLevelThresholdId.generate())
                .tenantId(command.getTenantId())
                .productId(command.getProductId())
                .locationId(command.getLocationId())
                .minimumQuantity(command.getMinimumQuantity())
                .maximumQuantity(command.getMaximumQuantity())
                .enableAutoRestock(command.isEnableAutoRestock())
                .build();
        }

        // 2. Persist
        repository.save(threshold);

        // 3. Return result
        return ConfigureStockLevelThresholdResult.builder()
            .thresholdId(threshold.getId())
            .minimumQuantity(threshold.getMinimumQuantity().getValue())
            .maximumQuantity(threshold.getMaximumQuantity().getValue())
            .build();
    }
}
```

### Event Listeners

```java
@Component
public class StockLevelValidationEventListener {

    private final StockLevelThresholdRepository thresholdRepository;
    private final StockManagementEventPublisher eventPublisher;

    @KafkaListener(
        topics = "stock-management-events",
        groupId = "stock-level-validation"
    )
    public void handleStockLevelChange(
        @Payload Map<String, Object> eventData,
        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String messageKey
    ) {
        String eventType = (String) eventData.get("eventType");

        if (isStockLevelChangeEvent(eventType)) {
            String productId = (String) eventData.get("productId");
            String locationId = (String) eventData.get("locationId");
            String tenantId = (String) eventData.get("tenantId");
            Integer currentQuantity = (Integer) eventData.get("currentQuantity");

            // Find applicable threshold
            Optional<StockLevelThreshold> threshold = findApplicableThreshold(
                ProductId.of(UUID.fromString(productId)),
                locationId != null ? LocationId.of(UUID.fromString(locationId)) : null,
                TenantId.of(UUID.fromString(tenantId))
            );

            if (threshold.isPresent()) {
                // Validate and publish events if thresholds violated
                threshold.get().validateStockLevel(currentQuantity);

                List<DomainEvent<?>> domainEvents =
                    new ArrayList<>(threshold.get().getDomainEvents());

                if (!domainEvents.isEmpty()) {
                    eventPublisher.publish(domainEvents);
                    threshold.get().clearDomainEvents();
                }
            }
        }
    }

    private Optional<StockLevelThreshold> findApplicableThreshold(
        ProductId productId,
        LocationId locationId,
        TenantId tenantId
    ) {
        // Try location-specific threshold first
        if (locationId != null) {
            Optional<StockLevelThreshold> locationThreshold =
                thresholdRepository.findByTenantIdAndProductIdAndLocationId(
                    tenantId, productId, locationId
                );
            if (locationThreshold.isPresent()) {
                return locationThreshold;
            }
        }

        // Fall back to product-wide threshold
        return thresholdRepository.findByTenantIdAndProductId(tenantId, productId);
    }
}
```

### Validation in Stock Adjustment

```java
@Component
public class AdjustStockCommandHandler {

    private final StockItemRepository stockItemRepository;
    private final StockLevelThresholdRepository thresholdRepository;

    @Transactional
    public AdjustStockResult handle(AdjustStockCommand command) {
        // 1. Load stock item
        StockItem stockItem = stockItemRepository.findById(command.getStockItemId())
            .orElseThrow(() -> new StockItemNotFoundException(...));

        // 2. Find applicable threshold
        Optional<StockLevelThreshold> threshold = findApplicableThreshold(
            stockItem.getProductId(),
            stockItem.getLocationId(),
            stockItem.getTenantId()
        );

        // 3. Validate against maximum if increasing
        if (command.getQuantityChange() > 0 && threshold.isPresent()) {
            int newQuantity = stockItem.getQuantity().getValue() + command.getQuantityChange();

            if (threshold.get().wouldExceedMaximum(
                command.getQuantityChange(),
                stockItem.getQuantity().getValue()
            )) {
                throw new MaximumStockLevelExceededException(
                    "Adjustment would exceed maximum stock level of " +
                    threshold.get().getMaximumQuantity().getValue()
                );
            }
        }

        // 4. Apply adjustment
        stockItem.adjustQuantity(command.getQuantityChange());

        // 5. Save and publish events
        // ... rest of handler logic
    }
}
```

---

## Database Migration

```sql
-- V8__Create_stock_level_thresholds_table.sql

CREATE TABLE stock_level_thresholds (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    location_id UUID,  -- NULL for warehouse-wide threshold
    minimum_quantity INTEGER NOT NULL CHECK (minimum_quantity >= 0),
    maximum_quantity INTEGER NOT NULL CHECK (maximum_quantity >= 0),
    enable_auto_restock BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_stock_level_threshold_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT chk_min_less_than_max CHECK (minimum_quantity < maximum_quantity),
    UNIQUE (tenant_id, product_id, location_id)  -- One threshold per product/location
);

-- Indexes
CREATE INDEX idx_stock_level_thresholds_tenant ON stock_level_thresholds(tenant_id);
CREATE INDEX idx_stock_level_thresholds_product ON stock_level_thresholds(product_id);
CREATE INDEX idx_stock_level_thresholds_location ON stock_level_thresholds(location_id);
```

---

## Testing Strategy

### Gateway API Tests

```java
@Test
public void shouldConfigureStockLevelThreshold() {
    ConfigureStockLevelThresholdRequest request =
        ConfigureStockLevelThresholdRequest.builder()
            .productId(productId)
            .minimumQuantity(10)
            .maximumQuantity(100)
            .enableAutoRestock(true)
            .build();

    authenticatedPost("/api/v1/stock-management/thresholds",
        accessToken, tenantId, request)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.success").isEqualTo(true)
        .jsonPath("$.data.minimumQuantity").isEqualTo(10)
        .jsonPath("$.data.maximumQuantity").isEqualTo(100);
}

@Test
public void shouldPreventExceedingMaximumStockLevel() {
    // Configure threshold
    configureThreshold(productId, 10, 100);

    // Try to adjust stock beyond maximum
    AdjustStockRequest request = AdjustStockRequest.builder()
        .stockItemId(stockItemId)
        .quantityChange(200)  // Would exceed maximum
        .reason("STOCK_COUNT")
        .build();

    authenticatedPost("/api/v1/stock-management/adjustments",
        accessToken, tenantId, request)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.message").value(containsString("maximum"));
}
```

---

## Acceptance Criteria Validation

- ✅ **AC1:** System maintains minimum and maximum levels per product (StockLevelThreshold aggregate)
- ✅ **AC2:** Levels may vary by location or warehouse (locationId nullable)
- ✅ **AC3:** System prevents exceeding maximum (Validation in command handlers)
- ✅ **AC4:** System alerts when approaching minimum (StockLevelBelowMinimumEvent)
- ✅ **AC5:** System validates thresholds when updating (validateStockLevel() method)

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
