# Monitor Stock Levels Implementation Plan

## US-5.1.1: Monitor Stock Levels

**Service:** Stock Management Service
**Priority:** Must Have
**Story Points:** 5
**Sprint:** Sprint 4

---

## Table of Contents

1. [Overview](#overview)
2. [UI Design](#ui-design)
3. [Domain Model Design](#domain-model-design)
4. [Backend Implementation](#backend-implementation)
5. [Frontend Implementation](#frontend-implementation)
6. [Data Flow](#data-flow)
7. [Testing Strategy](#testing-strategy)
8. [Acceptance Criteria Validation](#acceptance-criteria-validation)

---

## Overview

### User Story

**As a** warehouse manager
**I want** to monitor stock levels in real-time
**So that** I can make informed decisions about restocking and allocation

### Business Requirements

- System calculates stock levels in real-time
- Stock level visibility in dashboards
- Historical stock level tracking
- Stock level reports by product, location, warehouse
- System updates stock levels on stock movements, picking, returns

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Read model for optimized queries
- Real-time updates via event listeners
- Aggregated views by product, location, warehouse
- Historical tracking for trend analysis

---

## UI Design

### Stock Level Dashboard

**Component:** `StockLevelDashboard.tsx`

**Features:**

- **Real-Time Metrics** - Total stock, available stock, allocated stock
- **Stock by Location** - Grid view showing stock levels per location
- **Stock by Product** - List of products with current levels
- **Trend Charts** - Historical stock level trends
- **Low Stock Alerts** - Visual indicators for low stock items
- **Filter Options** - Filter by warehouse, product category, classification

**Dashboard Layout:**

```typescript
<DashboardLayout>
  {/* Summary Cards */}
  <Grid container spacing={3}>
    <Grid item xs={12} md={3}>
      <MetricCard
        title="Total Stock"
        value={stockMetrics.totalQuantity}
        icon={<Inventory />}
        trend={stockMetrics.trend}
      />
    </Grid>
    <Grid item xs={12} md={3}>
      <MetricCard
        title="Available Stock"
        value={stockMetrics.availableQuantity}
        icon={<CheckCircle />}
        color="success"
      />
    </Grid>
    <Grid item xs={12} md={3}>
      <MetricCard
        title="Allocated Stock"
        value={stockMetrics.allocatedQuantity}
        icon={<Lock />}
        color="warning"
      />
    </Grid>
    <Grid item xs={12} md={3}>
      <MetricCard
        title="Low Stock Items"
        value={stockMetrics.lowStockCount}
        icon={<Warning />}
        color="error"
      />
    </Grid>
  </Grid>

  {/* Stock Level Table */}
  <Grid container spacing={3}>
    <Grid item xs={12}>
      <Card>
        <CardHeader title="Stock Levels by Product" />
        <CardContent>
          <DataGrid
            rows={stockLevels}
            columns={[
              { field: 'productCode', headerName: 'Product Code', width: 150 },
              { field: 'productName', headerName: 'Product Name', width: 200 },
              { field: 'location', headerName: 'Location', width: 150 },
              {
                field: 'totalQuantity',
                headerName: 'Total',
                width: 100,
                align: 'right',
              },
              {
                field: 'availableQuantity',
                headerName: 'Available',
                width: 100,
                align: 'right',
              },
              {
                field: 'allocatedQuantity',
                headerName: 'Allocated',
                width: 100,
                align: 'right',
              },
              {
                field: 'status',
                headerName: 'Status',
                width: 120,
                renderCell: (params) => (
                  <Chip
                    label={params.value}
                    color={getStockLevelStatusColor(params.value)}
                    size="small"
                  />
                ),
              },
            ]}
            pageSize={10}
            rowsPerPageOptions={[10, 25, 50]}
          />
        </CardContent>
      </Card>
    </Grid>
  </Grid>

  {/* Stock Trend Chart */}
  <Grid container spacing={3}>
    <Grid item xs={12}>
      <Card>
        <CardHeader title="Stock Level Trends" />
        <CardContent>
          <LineChart
            data={stockTrends}
            xField="date"
            yField="quantity"
            series={[
              { dataKey: 'totalQuantity', name: 'Total', color: '#1976d2' },
              { dataKey: 'availableQuantity', name: 'Available', color: '#2e7d32' },
              { dataKey: 'allocatedQuantity', name: 'Allocated', color: '#ed6c02' },
            ]}
            height={300}
          />
        </CardContent>
      </Card>
    </Grid>
  </Grid>
</DashboardLayout>
```

**Stock Level Status Colors:**

- NORMAL: Green (stock above minimum)
- LOW: Yellow (stock approaching minimum)
- CRITICAL: Red (stock below minimum)
- OUT_OF_STOCK: Grey (zero stock)

---

## Domain Model Design

### StockLevel Value Object (Move to common-domain)

**Package:** `com.ccbsa.common.domain.valueobject`

```java
package com.ccbsa.common.domain.valueobject;

import java.time.LocalDateTime;

/**
 * Value Object: StockLevel
 * <p>
 * Represents aggregated stock level information.
 * Calculated from stock items and allocations.
 * <p>
 * This value object is shared across services (DRY principle).
 */
public final class StockLevel {
    private final int totalQuantity;      // Total stock (physical inventory)
    private final int availableQuantity;  // Available for allocation (total - allocated)
    private final int allocatedQuantity;  // Reserved for orders/picking
    private final int minimumQuantity;    // Minimum threshold (optional)
    private final int maximumQuantity;    // Maximum threshold (optional)
    private final LocalDateTime calculatedAt;

    private StockLevel(
        int totalQuantity,
        int availableQuantity,
        int allocatedQuantity,
        int minimumQuantity,
        int maximumQuantity,
        LocalDateTime calculatedAt
    ) {
        if (totalQuantity < 0) {
            throw new IllegalArgumentException("Total quantity cannot be negative");
        }
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("Available quantity cannot be negative");
        }
        if (allocatedQuantity < 0) {
            throw new IllegalArgumentException("Allocated quantity cannot be negative");
        }
        if (totalQuantity != (availableQuantity + allocatedQuantity)) {
            throw new IllegalArgumentException(
                "Total quantity must equal available + allocated"
            );
        }

        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.allocatedQuantity = allocatedQuantity;
        this.minimumQuantity = minimumQuantity;
        this.maximumQuantity = maximumQuantity;
        this.calculatedAt = calculatedAt != null ? calculatedAt : LocalDateTime.now();
    }

    public static StockLevel of(
        int totalQuantity,
        int availableQuantity,
        int allocatedQuantity
    ) {
        return new StockLevel(
            totalQuantity,
            availableQuantity,
            allocatedQuantity,
            0,
            Integer.MAX_VALUE,
            LocalDateTime.now()
        );
    }

    public static StockLevel of(
        int totalQuantity,
        int availableQuantity,
        int allocatedQuantity,
        int minimumQuantity,
        int maximumQuantity
    ) {
        return new StockLevel(
            totalQuantity,
            availableQuantity,
            allocatedQuantity,
            minimumQuantity,
            maximumQuantity,
            LocalDateTime.now()
        );
    }

    public boolean isBelowMinimum() {
        return totalQuantity < minimumQuantity;
    }

    public boolean isAboveMaximum() {
        return totalQuantity > maximumQuantity;
    }

    public boolean isOutOfStock() {
        return totalQuantity == 0;
    }

    public boolean hasAvailableStock() {
        return availableQuantity > 0;
    }

    public StockLevelStatus getStatus() {
        if (isOutOfStock()) {
            return StockLevelStatus.OUT_OF_STOCK;
        } else if (isBelowMinimum()) {
            return StockLevelStatus.CRITICAL;
        } else if (totalQuantity < (minimumQuantity * 1.2)) {
            return StockLevelStatus.LOW;
        } else {
            return StockLevelStatus.NORMAL;
        }
    }

    public int getAllocationCapacity() {
        return availableQuantity;
    }

    // Getters
    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public int getMinimumQuantity() {
        return minimumQuantity;
    }

    public int getMaximumQuantity() {
        return maximumQuantity;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockLevel that = (StockLevel) o;
        return totalQuantity == that.totalQuantity &&
               availableQuantity == that.availableQuantity &&
               allocatedQuantity == that.allocatedQuantity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalQuantity, availableQuantity, allocatedQuantity);
    }

    @Override
    public String toString() {
        return String.format(
            "StockLevel{total=%d, available=%d, allocated=%d, status=%s}",
            totalQuantity, availableQuantity, allocatedQuantity, getStatus()
        );
    }
}
```

### StockLevelStatus Enum (Move to common-domain)

**Package:** `com.ccbsa.common.domain.valueobject`

```java
package com.ccbsa.common.domain.valueobject;

/**
 * Value Object: StockLevelStatus
 * <p>
 * Enum representing status of stock level.
 * Shared across services (DRY principle).
 */
public enum StockLevelStatus {
    NORMAL("Normal stock level"),
    LOW("Low stock - approaching minimum"),
    CRITICAL("Critical stock - below minimum"),
    OUT_OF_STOCK("Out of stock");

    private final String description;

    StockLevelStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

### StockLevelSnapshot Entity (Read Model)

**Package:** `com.ccbsa.wms.stock.domain.core.entity`

```java
package com.ccbsa.wms.stock.domain.core.entity;

import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.common.domain.valueobject.StockLevel;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.stock.domain.core.valueobject.StockLevelSnapshotId;

import java.time.LocalDateTime;

/**
 * Entity: StockLevelSnapshot
 * <p>
 * Read model for stock level queries.
 * Updated via event listeners for eventual consistency.
 * <p>
 * Denormalized view optimized for reporting and dashboards.
 */
public class StockLevelSnapshot {

    private StockLevelSnapshotId id;
    private TenantId tenantId;
    private ProductId productId;
    private LocationId locationId;  // May be null for product-wide aggregation
    private StockLevel stockLevel;
    private LocalDateTime snapshotAt;
    private LocalDateTime lastUpdatedAt;

    // Product denormalized data for faster queries
    private String productCode;
    private String productName;

    // Location denormalized data for faster queries
    private String locationCode;
    private String locationName;

    private StockLevelSnapshot() {
        // Private constructor for builder
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Update stock level from events.
     */
    public void updateStockLevel(StockLevel newStockLevel) {
        this.stockLevel = newStockLevel;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // Getters
    public StockLevelSnapshotId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public StockLevel getStockLevel() {
        return stockLevel;
    }

    public LocalDateTime getSnapshotAt() {
        return snapshotAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public String getLocationName() {
        return locationName;
    }

    // Builder pattern
    public static class Builder {
        private StockLevelSnapshot snapshot = new StockLevelSnapshot();

        public Builder id(StockLevelSnapshotId id) {
            snapshot.id = id;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            snapshot.tenantId = tenantId;
            return this;
        }

        public Builder productId(ProductId productId) {
            snapshot.productId = productId;
            return this;
        }

        public Builder locationId(LocationId locationId) {
            snapshot.locationId = locationId;
            return this;
        }

        public Builder stockLevel(StockLevel stockLevel) {
            snapshot.stockLevel = stockLevel;
            return this;
        }

        public Builder productCode(String productCode) {
            snapshot.productCode = productCode;
            return this;
        }

        public Builder productName(String productName) {
            snapshot.productName = productName;
            return this;
        }

        public Builder locationCode(String locationCode) {
            snapshot.locationCode = locationCode;
            return this;
        }

        public Builder locationName(String locationName) {
            snapshot.locationName = locationName;
            return this;
        }

        public StockLevelSnapshot build() {
            validate();
            snapshot.snapshotAt = LocalDateTime.now();
            snapshot.lastUpdatedAt = LocalDateTime.now();
            return snapshot;
        }

        private void validate() {
            if (snapshot.id == null) {
                throw new IllegalArgumentException("StockLevelSnapshotId is required");
            }
            if (snapshot.tenantId == null) {
                throw new IllegalArgumentException("TenantId is required");
            }
            if (snapshot.productId == null) {
                throw new IllegalArgumentException("ProductId is required");
            }
            if (snapshot.stockLevel == null) {
                throw new IllegalArgumentException("StockLevel is required");
            }
        }
    }
}
```

---

## Backend Implementation

### Phase 1: Common Value Objects (common-domain)

**Files to Create:**

1. `StockLevel.java` - Value object for stock level aggregation
2. `StockLevelStatus.java` - Enum for stock level status

### Phase 2: Domain Core (stock-management-domain-core)

**Files to Create:**

1. `StockLevelSnapshot.java` - Read model entity
2. `StockLevelSnapshotId.java` - Value object (UUID-based)

### Phase 3: Application Service (stock-management-application-service)

**Query Handlers:**

```java
package com.ccbsa.wms.stock.application.service.query;

@Component
public class GetStockLevelsByProductAndLocationQueryHandler {

    private final StockLevelSnapshotRepository repository;

    @Transactional(readOnly = true)
    public GetStockLevelsByProductAndLocationQueryResult handle(
        GetStockLevelsByProductAndLocationQuery query
    ) {
        // 1. Validate query
        validateQuery(query);

        // 2. Query from read model
        List<StockLevelSnapshot> snapshots;

        if (query.getProductId() != null && query.getLocationId() != null) {
            // Specific product at specific location
            snapshots = repository.findByTenantIdAndProductIdAndLocationId(
                query.getTenantId(),
                query.getProductId(),
                query.getLocationId()
            );
        } else if (query.getProductId() != null) {
            // All locations for a product
            snapshots = repository.findByTenantIdAndProductId(
                query.getTenantId(),
                query.getProductId()
            );
        } else if (query.getLocationId() != null) {
            // All products at a location
            snapshots = repository.findByTenantIdAndLocationId(
                query.getTenantId(),
                query.getLocationId()
            );
        } else {
            // All stock levels for tenant
            snapshots = repository.findByTenantId(query.getTenantId());
        }

        // 3. Map to query results
        List<StockLevelQueryResult> results = snapshots.stream()
            .map(this::mapToQueryResult)
            .collect(Collectors.toList());

        // 4. Calculate totals
        int totalStock = results.stream()
            .mapToInt(r -> r.getStockLevel().getTotalQuantity())
            .sum();
        int availableStock = results.stream()
            .mapToInt(r -> r.getStockLevel().getAvailableQuantity())
            .sum();
        int allocatedStock = results.stream()
            .mapToInt(r -> r.getStockLevel().getAllocatedQuantity())
            .sum();

        return GetStockLevelsByProductAndLocationQueryResult.builder()
            .stockLevels(results)
            .totalCount(results.size())
            .totalStock(totalStock)
            .availableStock(availableStock)
            .allocatedStock(allocatedStock)
            .build();
    }

    private StockLevelQueryResult mapToQueryResult(StockLevelSnapshot snapshot) {
        return StockLevelQueryResult.builder()
            .productId(snapshot.getProductId())
            .productCode(snapshot.getProductCode())
            .productName(snapshot.getProductName())
            .locationId(snapshot.getLocationId())
            .locationCode(snapshot.getLocationCode())
            .locationName(snapshot.getLocationName())
            .stockLevel(snapshot.getStockLevel())
            .status(snapshot.getStockLevel().getStatus())
            .snapshotAt(snapshot.getSnapshotAt())
            .lastUpdatedAt(snapshot.getLastUpdatedAt())
            .build();
    }

    private void validateQuery(GetStockLevelsByProductAndLocationQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
    }
}
```

```java
package com.ccbsa.wms.stock.application.service.query;

@Component
public class GetLowStockItemsQueryHandler {

    private final StockLevelSnapshotRepository repository;

    @Transactional(readOnly = true)
    public GetLowStockItemsQueryResult handle(GetLowStockItemsQuery query) {
        // Query snapshots where stock is below minimum
        List<StockLevelSnapshot> lowStockSnapshots =
            repository.findByTenantIdAndStockLevelBelowMinimum(query.getTenantId());

        List<StockLevelQueryResult> results = lowStockSnapshots.stream()
            .map(this::mapToQueryResult)
            .sorted(Comparator.comparing(r ->
                r.getStockLevel().getTotalQuantity() - r.getStockLevel().getMinimumQuantity()
            ))
            .collect(Collectors.toList());

        return GetLowStockItemsQueryResult.builder()
            .lowStockItems(results)
            .totalCount(results.size())
            .build();
    }
}
```

### Phase 4: Event Listeners (stock-management-messaging)

**Stock Level Update Event Listener:**

```java
package com.ccbsa.wms.stock.messaging.listener;

@Component
public class StockLevelUpdateEventListener {

    private final UpdateStockLevelSnapshotCommandHandler updateSnapshotHandler;
    private final StockItemRepository stockItemRepository;
    private final StockAllocationRepository allocationRepository;

    @KafkaListener(
        topics = "stock-management-events",
        groupId = "stock-level-snapshot-updater"
    )
    public void handleStockEvent(
        @Payload Map<String, Object> eventData,
        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String messageKey
    ) {
        try {
            String eventType = (String) eventData.get("eventType");

            if (isStockLevelAffectingEvent(eventType)) {
                String productId = (String) eventData.get("productId");
                String locationId = (String) eventData.get("locationId");
                String tenantId = (String) eventData.get("tenantId");

                // Recalculate stock level for this product/location
                recalculateStockLevel(
                    ProductId.of(UUID.fromString(productId)),
                    locationId != null ? LocationId.of(UUID.fromString(locationId)) : null,
                    TenantId.of(UUID.fromString(tenantId))
                );
            }
        } catch (Exception e) {
            logger.error("Error processing stock level update event", e);
            throw e;
        }
    }

    private boolean isStockLevelAffectingEvent(String eventType) {
        return eventType.equals("StockItemCreatedEvent") ||
               eventType.equals("StockAdjustedEvent") ||
               eventType.equals("StockAllocatedEvent") ||
               eventType.equals("StockAllocationReleasedEvent") ||
               eventType.equals("StockMovementCompletedEvent");
    }

    private void recalculateStockLevel(
        ProductId productId,
        LocationId locationId,
        TenantId tenantId
    ) {
        // 1. Query all stock items for product/location
        List<StockItem> stockItems = locationId != null
            ? stockItemRepository.findByTenantIdAndProductIdAndLocationId(
                tenantId, productId, locationId
              )
            : stockItemRepository.findByTenantIdAndProductId(tenantId, productId);

        // 2. Calculate total quantity
        int totalQuantity = stockItems.stream()
            .mapToInt(item -> item.getQuantity().getValue())
            .sum();

        // 3. Query allocations
        List<StockAllocation> allocations = locationId != null
            ? allocationRepository.findByTenantIdAndProductIdAndLocationId(
                tenantId, productId, locationId
              )
            : allocationRepository.findByTenantIdAndProductId(tenantId, productId);

        // 4. Calculate allocated quantity
        int allocatedQuantity = allocations.stream()
            .filter(allocation -> allocation.getStatus() == AllocationStatus.ALLOCATED)
            .mapToInt(allocation -> allocation.getQuantity().getValue())
            .sum();

        // 5. Calculate available quantity
        int availableQuantity = totalQuantity - allocatedQuantity;

        // 6. Create or update snapshot
        StockLevel stockLevel = StockLevel.of(
            totalQuantity,
            availableQuantity,
            allocatedQuantity
        );

        updateSnapshotHandler.handle(
            UpdateStockLevelSnapshotCommand.builder()
                .tenantId(tenantId)
                .productId(productId)
                .locationId(locationId)
                .stockLevel(stockLevel)
                .build()
        );
    }
}
```

### Phase 5: Data Access (stock-management-dataaccess)

**Database Migration:**

```sql
-- V7__Create_stock_level_snapshots_table.sql

CREATE TABLE stock_level_snapshots (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    location_id UUID,  -- NULL for product-wide aggregation
    total_quantity INTEGER NOT NULL CHECK (total_quantity >= 0),
    available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0),
    allocated_quantity INTEGER NOT NULL CHECK (allocated_quantity >= 0),
    minimum_quantity INTEGER DEFAULT 0,
    maximum_quantity INTEGER DEFAULT 2147483647,
    snapshot_at TIMESTAMP NOT NULL,
    last_updated_at TIMESTAMP NOT NULL,
    -- Denormalized product data
    product_code VARCHAR(50),
    product_name VARCHAR(255),
    -- Denormalized location data
    location_code VARCHAR(50),
    location_name VARCHAR(255),
    CONSTRAINT fk_stock_level_snapshot_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT chk_quantity_consistency CHECK (total_quantity = available_quantity + allocated_quantity),
    UNIQUE (tenant_id, product_id, location_id)  -- One snapshot per product/location combination
);

-- Indexes for performance
CREATE INDEX idx_stock_level_snapshots_tenant ON stock_level_snapshots(tenant_id);
CREATE INDEX idx_stock_level_snapshots_product ON stock_level_snapshots(product_id);
CREATE INDEX idx_stock_level_snapshots_location ON stock_level_snapshots(location_id);
CREATE INDEX idx_stock_level_snapshots_low_stock ON stock_level_snapshots(total_quantity)
    WHERE total_quantity < minimum_quantity;
CREATE INDEX idx_stock_level_snapshots_updated ON stock_level_snapshots(last_updated_at DESC);
```

---

## Frontend Implementation

### Stock Level Service

**File:** `frontend-app/src/features/stock-management/services/stockLevelService.ts`

```typescript
import { apiClient } from '../../../services/apiClient';
import { ApiResponse } from '../../../types/api';

export interface StockLevelQueryResult {
  productId: string;
  productCode: string;
  productName: string;
  locationId?: string;
  locationCode?: string;
  locationName?: string;
  stockLevel: {
    totalQuantity: number;
    availableQuantity: number;
    allocatedQuantity: number;
    minimumQuantity: number;
    maximumQuantity: number;
  };
  status: 'NORMAL' | 'LOW' | 'CRITICAL' | 'OUT_OF_STOCK';
  snapshotAt: string;
  lastUpdatedAt: string;
}

export interface StockLevelSummary {
  stockLevels: StockLevelQueryResult[];
  totalCount: number;
  totalStock: number;
  availableStock: number;
  allocatedStock: number;
}

const BASE_PATH = '/api/v1/stock-management/stock-levels';

export const stockLevelService = {
  getStockLevels: async (
    filters: {
      productId?: string;
      locationId?: string;
    },
    tenantId: string
  ): Promise<ApiResponse<StockLevelSummary>> => {
    const params = new URLSearchParams();
    if (filters.productId) params.append('productId', filters.productId);
    if (filters.locationId) params.append('locationId', filters.locationId);

    const response = await apiClient.get(
      `${BASE_PATH}?${params.toString()}`,
      {
        headers: { 'X-Tenant-Id': tenantId },
      }
    );
    return response.data;
  },

  getLowStockItems: async (
    tenantId: string
  ): Promise<ApiResponse<StockLevelQueryResult[]>> => {
    const response = await apiClient.get(`${BASE_PATH}/low-stock`, {
      headers: { 'X-Tenant-Id': tenantId },
    });
    return response.data;
  },

  getStockLevelTrends: async (
    productId: string,
    days: number,
    tenantId: string
  ): Promise<ApiResponse<StockLevelTrend[]>> => {
    const response = await apiClient.get(
      `${BASE_PATH}/trends?productId=${productId}&days=${days}`,
      {
        headers: { 'X-Tenant-Id': tenantId },
      }
    );
    return response.data;
  },
};
```

---

## Data Flow

### Stock Level Calculation Flow

```
Stock Event (StockItemCreated, StockAdjusted, StockAllocated, etc.)
  ↓ Published to Kafka Topic: stock-management-events
StockLevelUpdateEventListener
  ↓ Consume event
  ↓ Extract productId, locationId, tenantId
Recalculate Stock Level
  ↓ Query all stock items for product/location
  ↓ Calculate total quantity (sum of all stock items)
  ↓ Query all allocations for product/location
  ↓ Calculate allocated quantity (sum of active allocations)
  ↓ Calculate available quantity (total - allocated)
UpdateStockLevelSnapshotCommandHandler
  ↓ Create or update StockLevelSnapshot
  ↓ Save to database
Read Model Updated

Frontend Query
  ↓ GET /api/v1/stock-management/stock-levels
Gateway Service
  ↓ Route to Stock Management Service
Stock Management Service (Query Controller)
  ↓ GetStockLevelsByProductAndLocationQuery
GetStockLevelsByProductAndLocationQueryHandler
  ↓ Query StockLevelSnapshot (read model)
  ↓ Map to StockLevelQueryResult
  ↓ Return aggregated results
```

---

## Testing Strategy

### Unit Tests

1. **StockLevel Value Object Tests**
    - Test creation with valid values
    - Test validation (total = available + allocated)
    - Test status calculation (NORMAL, LOW, CRITICAL, OUT_OF_STOCK)
    - Test capacity checks

2. **Query Handler Tests**
    - GetStockLevelsByProductAndLocationQueryHandler
    - GetLowStockItemsQueryHandler
    - Test aggregation logic
    - Test filtering by product/location

### Integration Tests

1. **Event Listener Tests**
    - Test stock level recalculation on events
    - Test snapshot creation/update
    - Verify eventual consistency

2. **Repository Tests**
    - Test snapshot persistence
    - Test queries by product/location
    - Test low stock queries

### Gateway API Tests

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockLevelTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    public void shouldGetStockLevelsByProduct() {
        authenticatedGet("/api/v1/stock-management/stock-levels?productId=" + productId,
            accessToken, tenantId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.stockLevels").isArray()
            .jsonPath("$.data.totalStock").isNumber()
            .jsonPath("$.data.availableStock").isNumber()
            .jsonPath("$.data.allocatedStock").isNumber();
    }

    @Test
    @Order(2)
    public void shouldGetLowStockItems() {
        authenticatedGet("/api/v1/stock-management/stock-levels/low-stock",
            accessToken, tenantId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray();
    }

    @Test
    @Order(3)
    public void shouldCalculateStockLevelCorrectly() {
        // Create stock item with quantity 100
        createStockItem(productId, locationId, 100);

        // Allocate 30
        allocateStock(productId, locationId, 30);

        // Query stock level
        EntityExchangeResult<ApiResponse<StockLevelSummary>> result =
            authenticatedGet("/api/v1/stock-management/stock-levels?productId=" + productId,
                accessToken, tenantId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<StockLevelSummary>>() {})
                .returnResult();

        StockLevelSummary summary = result.getResponseBody().getData();
        assertThat(summary.getTotalStock()).isEqualTo(100);
        assertThat(summary.getAllocatedStock()).isEqualTo(30);
        assertThat(summary.getAvailableStock()).isEqualTo(70);
    }
}
```

---

## Acceptance Criteria Validation

- ✅ **AC1:** System calculates stock levels in real-time (Event-driven recalculation)
- ✅ **AC2:** Stock level visibility in dashboards (StockLevelDashboard component)
- ✅ **AC3:** Historical stock level tracking (StockLevelSnapshot with timestamps)
- ✅ **AC4:** Stock level reports by product, location, warehouse (Query handlers with filters)
- ✅ **AC5:** System updates stock levels on movements/picking/returns (Event listeners)

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft
