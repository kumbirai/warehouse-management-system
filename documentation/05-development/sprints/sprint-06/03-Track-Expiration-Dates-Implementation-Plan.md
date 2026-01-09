# US-2.1.3: Track Expiration Dates and Generate Alerts Implementation Plan

## User Story

**Story ID:** US-2.1.3
**Title:** Track Expiration Dates and Generate Alerts
**Epic:** Stock Classification and Expiration Management
**Service:** Stock Management Service, Notification Service
**Priority:** Must Have
**Story Points:** 5

**As a** warehouse manager
**I want** the system to track stock expiration dates and generate alerts
**So that** I can prevent expired stock from being shipped and minimize waste

---

## Acceptance Criteria

- [ ] AC1: System generates alert when stock is within 7 days of expiration (CRITICAL)
- [ ] AC2: System generates alert when stock is within 30 days of expiration (NEAR_EXPIRY)
- [ ] AC3: System generates alert when stock has expired
- [ ] AC4: System prevents picking of expired stock
- [ ] AC5: System generates reports on expiring stock
- [ ] AC6: Alerts are visible in dashboard and can be filtered by date range
- [ ] AC7: System runs scheduled job daily to check expirations

---

## Table of Contents

1. [Overview](#overview)
2. [Frontend Implementation](#frontend-implementation)
3. [Backend Implementation](#backend-implementation)
4. [Domain Model](#domain-model)
5. [Scheduled Jobs](#scheduled-jobs)
6. [Event Design](#event-design)
7. [Notification Integration](#notification-integration)
8. [Testing Strategy](#testing-strategy)
9. [Implementation Checklist](#implementation-checklist)

---

## Overview

### Business Context

Expiration tracking is critical for:

1. **Food Safety** - Prevent expired products from being shipped to customers
2. **Regulatory Compliance** - Meet food safety regulations
3. **Waste Reduction** - Identify expiring stock early for clearance/disposal
4. **Inventory Optimization** - Prioritize expiring stock in picking (FEFO)
5. **Financial Impact** - Minimize write-offs from expired stock

### Classification Levels

- **CRITICAL (0-7 days)** - Stock expires within 7 days, urgent action required
- **NEAR_EXPIRY (8-30 days)** - Stock expires within 30 days, plan clearance
- **NORMAL (30+ days)** - Stock has adequate shelf life
- **EXPIRED** - Stock past expiration date, must not be picked

### Technical Approach

- **Scheduled Job:** Daily batch job checks all stock items for expiration
- **Event-Driven:** Publish events for expiring/expired stock
- **Dashboard Integration:** Real-time alerts visible to warehouse managers
- **FEFO Enforcement:** Picking queries exclude expired stock automatically

---

## Frontend Implementation

### UI Component: ExpiringStockDashboard

**Location:** `frontend-app/src/features/stock-management/pages/ExpiringStockDashboard.tsx`

```typescript
import React, { useState, useEffect } from 'react';
import { useExpiringStock } from '../hooks/useExpiringStock';
import { ExpiringStockList } from '../components/ExpiringStockList';
import { ExpirationAlertCard } from '../components/ExpirationAlertCard';
import { Alert } from '../../common/Alert';
import { Button } from '../../common/Button';
import { LoadingSpinner } from '../../common/LoadingSpinner';
import { DateRangePicker } from '../../common/DateRangePicker';

export const ExpiringStockDashboard: React.FC = () => {
  const [dateRange, setDateRange] = useState({
    startDate: new Date(),
    endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) // 30 days
  });

  const [selectedClassification, setSelectedClassification] = useState<string>('ALL');

  const {
    expiringStock,
    loading,
    error,
    refetch
  } = useExpiringStock(dateRange, selectedClassification);

  const criticalCount = expiringStock?.filter(
    s => s.classification === 'CRITICAL'
  ).length || 0;

  const nearExpiryCount = expiringStock?.filter(
    s => s.classification === 'NEAR_EXPIRY'
  ).length || 0;

  const expiredCount = expiringStock?.filter(
    s => s.classification === 'EXPIRED'
  ).length || 0;

  useEffect(() => {
    // Auto-refresh every 5 minutes
    const interval = setInterval(() => {
      refetch();
    }, 5 * 60 * 1000);

    return () => clearInterval(interval);
  }, [refetch]);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <Alert type="error" message={error} />
      </div>
    );
  }

  return (
    <div className="container mx-auto p-6">
      {/* Page Header */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Expiring Stock Dashboard</h1>
        <p className="text-gray-600 mt-2">
          Monitor stock expiration dates and take action to minimize waste
        </p>
      </div>

      {/* Alert Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <ExpirationAlertCard
          title="Critical (0-7 days)"
          count={criticalCount}
          level="CRITICAL"
          onClick={() => setSelectedClassification('CRITICAL')}
        />
        <ExpirationAlertCard
          title="Near Expiry (8-30 days)"
          count={nearExpiryCount}
          level="NEAR_EXPIRY"
          onClick={() => setSelectedClassification('NEAR_EXPIRY')}
        />
        <ExpirationAlertCard
          title="Expired"
          count={expiredCount}
          level="EXPIRED"
          onClick={() => setSelectedClassification('EXPIRED')}
        />
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <div className="flex flex-wrap gap-4 items-end">
          <div className="flex-1 min-w-[300px]">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Date Range
            </label>
            <DateRangePicker
              startDate={dateRange.startDate}
              endDate={dateRange.endDate}
              onChange={setDateRange}
            />
          </div>

          <div className="flex-1 min-w-[200px]">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Classification
            </label>
            <select
              value={selectedClassification}
              onChange={(e) => setSelectedClassification(e.target.value)}
              className="w-full border border-gray-300 rounded-md p-2"
            >
              <option value="ALL">All Classifications</option>
              <option value="CRITICAL">Critical (0-7 days)</option>
              <option value="NEAR_EXPIRY">Near Expiry (8-30 days)</option>
              <option value="EXPIRED">Expired</option>
            </select>
          </div>

          <Button
            variant="secondary"
            onClick={refetch}
            className="whitespace-nowrap"
          >
            Refresh Data
          </Button>
        </div>
      </div>

      {/* Expiring Stock List */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-xl font-semibold mb-4">
          Expiring Stock Items
          {selectedClassification !== 'ALL' && ` - ${selectedClassification}`}
        </h2>

        {expiringStock && expiringStock.length > 0 ? (
          <ExpiringStockList
            items={expiringStock}
            onItemClick={(item) => {
              // Navigate to stock item detail
              window.location.href = `/stock/stock-items/${item.stockItemId}`;
            }}
          />
        ) : (
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg">
              No expiring stock items found for the selected criteria
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
```

### Supporting Components

**ExpirationAlertCard:**

```typescript
import React from 'react';

interface ExpirationAlertCardProps {
  title: string;
  count: number;
  level: 'CRITICAL' | 'NEAR_EXPIRY' | 'EXPIRED';
  onClick: () => void;
}

export const ExpirationAlertCard: React.FC<ExpirationAlertCardProps> = ({
  title,
  count,
  level,
  onClick
}) => {
  const getCardStyles = () => {
    switch (level) {
      case 'CRITICAL':
        return 'bg-red-50 border-red-200 hover:bg-red-100';
      case 'NEAR_EXPIRY':
        return 'bg-yellow-50 border-yellow-200 hover:bg-yellow-100';
      case 'EXPIRED':
        return 'bg-gray-50 border-gray-300 hover:bg-gray-100';
    }
  };

  const getIconColor = () => {
    switch (level) {
      case 'CRITICAL':
        return 'text-red-600';
      case 'NEAR_EXPIRY':
        return 'text-yellow-600';
      case 'EXPIRED':
        return 'text-gray-600';
    }
  };

  return (
    <div
      className={`${getCardStyles()} border-2 rounded-lg p-6 cursor-pointer transition-colors`}
      onClick={onClick}
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-700">{title}</p>
          <p className={`text-4xl font-bold mt-2 ${getIconColor()}`}>
            {count}
          </p>
        </div>
        <div className={`text-5xl ${getIconColor()}`}>
          {level === 'CRITICAL' && '⚠️'}
          {level === 'NEAR_EXPIRY' && '⏰'}
          {level === 'EXPIRED' && '❌'}
        </div>
      </div>
    </div>
  );
};
```

**ExpiringStockList:**

```typescript
import React from 'react';
import { ExpiringStockItem } from '../types/stock';

interface ExpiringStockListProps {
  items: ExpiringStockItem[];
  onItemClick: (item: ExpiringStockItem) => void;
}

export const ExpiringStockList: React.FC<ExpiringStockListProps> = ({
  items,
  onItemClick
}) => {
  const getDaysUntilExpiration = (expirationDate: string): number => {
    const expDate = new Date(expirationDate);
    const today = new Date();
    const diffTime = expDate.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  const getClassificationBadge = (classification: string) => {
    const styles = {
      CRITICAL: 'bg-red-100 text-red-800 border-red-300',
      NEAR_EXPIRY: 'bg-yellow-100 text-yellow-800 border-yellow-300',
      EXPIRED: 'bg-gray-100 text-gray-800 border-gray-300',
      NORMAL: 'bg-green-100 text-green-800 border-green-300'
    };

    return (
      <span className={`px-3 py-1 rounded-full text-xs font-medium border ${styles[classification]}`}>
        {classification.replace('_', ' ')}
      </span>
    );
  };

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Product Code
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Product Description
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Location
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Quantity
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Expiration Date
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Days Remaining
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Classification
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {items.map((item) => {
            const daysRemaining = getDaysUntilExpiration(item.expirationDate);
            return (
              <tr
                key={item.stockItemId}
                onClick={() => onItemClick(item)}
                className="hover:bg-gray-50 cursor-pointer"
              >
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {item.productCode}
                </td>
                <td className="px-6 py-4 text-sm text-gray-900">
                  {item.productDescription}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {item.locationCode}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {item.quantity}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {new Date(item.expirationDate).toLocaleDateString()}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <span className={daysRemaining < 0 ? 'text-red-600 font-bold' : 
                                   daysRemaining <= 7 ? 'text-red-600' :
                                   daysRemaining <= 30 ? 'text-yellow-600' : 
                                   'text-green-600'}>
                    {daysRemaining < 0 ? 'EXPIRED' : `${daysRemaining} days`}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  {getClassificationBadge(item.classification)}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};
```

### Custom Hook: useExpiringStock

```typescript
import { useState, useEffect } from 'react';
import { apiClient } from '../../../services/apiClient';
import { ExpiringStockItem } from '../types/stock';

interface DateRange {
  startDate: Date;
  endDate: Date;
}

export const useExpiringStock = (
  dateRange: DateRange,
  classification: string
) => {
  const [expiringStock, setExpiringStock] = useState<ExpiringStockItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchExpiringStock = async () => {
    try {
      setLoading(true);
      setError(null);

      const params = new URLSearchParams({
        startDate: dateRange.startDate.toISOString().split('T')[0],
        endDate: dateRange.endDate.toISOString().split('T')[0],
      });

      if (classification !== 'ALL') {
        params.append('classification', classification);
      }

      const response = await apiClient.get<ExpiringStockItem[]>(
        `/api/v1/stock/stock-items/expiring?${params.toString()}`
      );

      setExpiringStock(response.data);
    } catch (err: any) {
      console.error('Failed to fetch expiring stock:', err);
      setError(err.response?.data?.message || 'Failed to load expiring stock');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchExpiringStock();
  }, [dateRange, classification]);

  return {
    expiringStock,
    loading,
    error,
    refetch: fetchExpiringStock
  };
};
```

---

## Backend Implementation

### Scheduled Job: ExpirationCheckScheduler

**Location:** `stock-management-container/src/main/java/com/ccbsa/wms/stock/scheduler/ExpirationCheckScheduler.java`

```java
package com.ccbsa.wms.stock.scheduler;

import com.ccbsa.wms.stock.application.service.command.CheckExpirationDatesCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpirationCheckScheduler {

    private final CheckExpirationDatesCommandHandler checkExpirationDatesCommandHandler;

    /**
     * Check stock expiration dates daily at 2:00 AM
     */
    @Scheduled(cron = "${stock.expiration.check.cron:0 0 2 * * *}")
    public void checkExpirationDates() {
        log.info("Starting scheduled expiration check...");

        try {
            checkExpirationDatesCommandHandler.handle();
            log.info("Expiration check completed successfully");
        } catch (Exception e) {
            log.error("Expiration check failed", e);
        }
    }
}
```

### Command Handler: CheckExpirationDatesCommandHandler

**Location:** `stock-management-application-service/src/main/java/com/ccbsa/wms/stock/application/service/command/CheckExpirationDatesCommandHandler.java`

```java
package com.ccbsa.wms.stock.application.service.command;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.stock.application.service.port.messaging.StockManagementEventPublisher;
import com.ccbsa.wms.stock.application.service.port.repository.StockItemRepository;
import com.ccbsa.wms.stock.domain.core.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.StockClassification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckExpirationDatesCommandHandler {

    private final StockItemRepository stockItemRepository;
    private final StockManagementEventPublisher eventPublisher;

    @Transactional
    public void handle() {
        log.info("Checking expiration dates for all stock items");

        // Get all stock items with expiration dates
        List<StockItem> stockItems = stockItemRepository.findAllWithExpirationDates();

        log.info("Found {} stock items to check", stockItems.size());

        int criticalCount = 0;
        int nearExpiryCount = 0;
        int expiredCount = 0;

        for (StockItem stockItem : stockItems) {
            // Check expiration and update classification
            StockClassification previousClassification = stockItem.getClassification();
            stockItem.checkExpiration();

            // If classification changed, save and publish events
            if (!stockItem.getClassification().equals(previousClassification)) {
                stockItemRepository.save(stockItem);

                List<DomainEvent<?>> events = stockItem.getDomainEvents();
                events.forEach(eventPublisher::publish);
                stockItem.clearDomainEvents();

                // Count by classification
                switch (stockItem.getClassification()) {
                    case CRITICAL:
                        criticalCount++;
                        break;
                    case NEAR_EXPIRY:
                        nearExpiryCount++;
                        break;
                    case EXPIRED:
                        expiredCount++;
                        break;
                }
            }
        }

        log.info("Expiration check completed. Critical: {}, Near Expiry: {}, Expired: {}",
                criticalCount, nearExpiryCount, expiredCount);
    }
}
```

### Query: GetExpiringStockQuery

**Location:** `stock-management-application/src/main/java/com/ccbsa/wms/stock/application/query/StockItemQueryController.java`

```java
@GetMapping("/stock-items/expiring")
public ResponseEntity<ApiResponse<List<ExpiringStockItemQueryDTO>>> getExpiringStock(
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate,
        @RequestParam(required = false) StockClassification classification) {

    log.info("Getting expiring stock from {} to {} with classification: {}",
            startDate, endDate, classification);

    GetExpiringStockQuery query = GetExpiringStockQuery.builder()
            .startDate(startDate)
            .endDate(endDate)
            .classification(classification)
            .build();

    List<ExpiringStockItemQueryDTO> result = getExpiringStockQueryHandler.handle(query);

    return ResponseEntity.ok(ApiResponseBuilder.ok(result));
}
```

---

## Domain Model

### StockItem Aggregate - Expiration Methods

**Location:** `stock-management-domain-core/src/main/java/com/ccbsa/wms/stock/domain/core/StockItem.java`

```java
package com.ccbsa.wms.stock.domain.core;

import com.ccbsa.common.domain.AggregateRoot;
import com.ccbsa.wms.stock.domain.core.event.StockClassifiedEvent;
import com.ccbsa.wms.stock.domain.core.event.StockExpiringAlertEvent;
import com.ccbsa.wms.stock.domain.core.event.StockExpiredEvent;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
public class StockItem extends AggregateRoot<StockItemId> {

    private ProductId productId;
    private LocationId locationId;
    private Quantity quantity;
    private ExpirationDate expirationDate;
    private StockClassification classification;
    private LocalDate lastCheckedDate;

    /**
     * Check expiration and update classification
     */
    public void checkExpiration() {
        if (expirationDate == null) {
            return; // No expiration date to check
        }

        LocalDate today = LocalDate.now();
        LocalDate expDate = expirationDate.getValue();
        long daysUntilExpiration = ChronoUnit.DAYS.between(today, expDate);

        StockClassification previousClassification = this.classification;
        StockClassification newClassification;

        // Determine classification based on days until expiration
        if (daysUntilExpiration < 0) {
            newClassification = StockClassification.EXPIRED;
        } else if (daysUntilExpiration <= 7) {
            newClassification = StockClassification.CRITICAL;
        } else if (daysUntilExpiration <= 30) {
            newClassification = StockClassification.NEAR_EXPIRY;
        } else {
            newClassification = StockClassification.NORMAL;
        }

        // Update classification if changed
        if (!newClassification.equals(previousClassification)) {
            this.classification = newClassification;
            this.lastCheckedDate = today;

            // Publish appropriate event
            publishExpirationEvent(previousClassification, newClassification, daysUntilExpiration);
        }
    }

    private void publishExpirationEvent(StockClassification previousClassification,
                                       StockClassification newClassification,
                                       long daysUntilExpiration) {

        if (newClassification == StockClassification.EXPIRED) {
            addDomainEvent(new StockExpiredEvent(
                    this.id,
                    this.productId,
                    this.locationId,
                    this.quantity,
                    this.expirationDate
            ));
        } else if (newClassification == StockClassification.CRITICAL ||
                   newClassification == StockClassification.NEAR_EXPIRY) {
            addDomainEvent(new StockExpiringAlertEvent(
                    this.id,
                    this.productId,
                    this.locationId,
                    this.quantity,
                    this.expirationDate,
                    newClassification,
                    (int) daysUntilExpiration
            ));
        }

        // Always publish classification changed event
        addDomainEvent(new StockClassifiedEvent(
                this.id,
                this.productId,
                previousClassification,
                newClassification
        ));
    }

    /**
     * Check if stock can be picked (not expired)
     */
    public boolean canBePicked() {
        return this.classification != StockClassification.EXPIRED;
    }

    /**
     * Check if stock is expiring soon
     */
    public boolean isExpiringSoon() {
        return this.classification == StockClassification.CRITICAL ||
               this.classification == StockClassification.NEAR_EXPIRY;
    }
}
```

### StockClassification Enum

```java
package com.ccbsa.wms.stock.domain.core.valueobject;

public enum StockClassification {
    NORMAL("Normal", "Stock with adequate shelf life (30+ days)"),
    NEAR_EXPIRY("Near Expiry", "Stock expiring within 30 days"),
    CRITICAL("Critical", "Stock expiring within 7 days - urgent action required"),
    EXPIRED("Expired", "Stock past expiration date - cannot be picked");

    private final String displayName;
    private final String description;

    StockClassification(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isExpired() {
        return this == EXPIRED;
    }

    public boolean isExpiringSoon() {
        return this == CRITICAL || this == NEAR_EXPIRY;
    }
}
```

---

## Event Design

### StockExpiringAlertEvent

```java
package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.wms.stock.domain.core.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import lombok.Getter;

@Getter
public class StockExpiringAlertEvent extends StockManagementEvent<StockItem> {

    private final ProductId productId;
    private final LocationId locationId;
    private final Quantity quantity;
    private final ExpirationDate expirationDate;
    private final StockClassification classification;
    private final int daysUntilExpiration;

    public StockExpiringAlertEvent(
            StockItemId stockItemId,
            ProductId productId,
            LocationId locationId,
            Quantity quantity,
            ExpirationDate expirationDate,
            StockClassification classification,
            int daysUntilExpiration) {
        super(stockItemId);
        this.productId = productId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
        this.classification = classification;
        this.daysUntilExpiration = daysUntilExpiration;
    }
}
```

### StockExpiredEvent

```java
package com.ccbsa.wms.stock.domain.core.event;

import com.ccbsa.wms.stock.domain.core.StockItem;
import com.ccbsa.wms.stock.domain.core.valueobject.*;
import lombok.Getter;

@Getter
public class StockExpiredEvent extends StockManagementEvent<StockItem> {

    private final ProductId productId;
    private final LocationId locationId;
    private final Quantity quantity;
    private final ExpirationDate expirationDate;

    public StockExpiredEvent(
            StockItemId stockItemId,
            ProductId productId,
            LocationId locationId,
            Quantity quantity,
            ExpirationDate expirationDate) {
        super(stockItemId);
        this.productId = productId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
    }
}
```

---

## Notification Integration

### Event Listener in Notification Service

**Location:** `notification-service/notification-messaging/src/main/java/com/ccbsa/wms/notification/messaging/listener/StockExpirationEventListener.java`

```java
package com.ccbsa.wms.notification.messaging.listener;

import com.ccbsa.wms.notification.application.service.command.CreateNotificationCommandHandler;
import com.ccbsa.wms.notification.application.command.dto.CreateNotificationCommand;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationPriority;
import com.ccbsa.wms.notification.domain.core.valueobject.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockExpirationEventListener {

    private final CreateNotificationCommandHandler createNotificationCommandHandler;

    @KafkaListener(
            topics = "stock-management-events",
            groupId = "notification-service-stock-expiration"
    )
    public void handleStockExpiringAlert(StockExpiringAlertEvent event) {
        log.info("Received StockExpiringAlertEvent for stock item: {}", 
                event.getAggregateId());

        NotificationPriority priority = event.getClassification() == StockClassification.CRITICAL
                ? NotificationPriority.HIGH
                : NotificationPriority.MEDIUM;

        String message = String.format(
                "Stock expiring in %d days: Product %s at location %s (Quantity: %d)",
                event.getDaysUntilExpiration(),
                event.getProductId(),
                event.getLocationId(),
                event.getQuantity().getValue()
        );

        CreateNotificationCommand command = CreateNotificationCommand.builder()
                .type(NotificationType.STOCK_EXPIRING)
                .priority(priority)
                .title("Stock Expiring Alert")
                .message(message)
                .targetUserId(null) // Send to all warehouse managers
                .build();

        createNotificationCommandHandler.handle(command);
    }

    @KafkaListener(
            topics = "stock-management-events",
            groupId = "notification-service-stock-expired"
    )
    public void handleStockExpired(StockExpiredEvent event) {
        log.info("Received StockExpiredEvent for stock item: {}",
                event.getAggregateId());

        String message = String.format(
                "Stock EXPIRED: Product %s at location %s (Quantity: %d). " +
                "Cannot be picked. Immediate action required.",
                event.getProductId(),
                event.getLocationId(),
                event.getQuantity().getValue()
        );

        CreateNotificationCommand command = CreateNotificationCommand.builder()
                .type(NotificationType.STOCK_EXPIRED)
                .priority(NotificationPriority.CRITICAL)
                .title("Stock Expired - Action Required")
                .message(message)
                .targetUserId(null) // Send to all warehouse managers
                .build();

        createNotificationCommandHandler.handle(command);
    }
}
```

---

## Testing Strategy

### Unit Tests - Domain Core

```java
@Test
void shouldClassifyStockAsCriticalWhen7DaysUntilExpiration() {
    // Given
    LocalDate expirationDate = LocalDate.now().plusDays(7);
    StockItem stockItem = createStockItem(expirationDate);

    // When
    stockItem.checkExpiration();

    // Then
    assertThat(stockItem.getClassification()).isEqualTo(StockClassification.CRITICAL);
    assertThat(stockItem.getDomainEvents()).hasSize(2); // StockExpiringAlertEvent + StockClassifiedEvent
}

@Test
void shouldClassifyStockAsExpiredWhenPastExpirationDate() {
    // Given
    LocalDate expirationDate = LocalDate.now().minusDays(1);
    StockItem stockItem = createStockItem(expirationDate);

    // When
    stockItem.checkExpiration();

    // Then
    assertThat(stockItem.getClassification()).isEqualTo(StockClassification.EXPIRED);
    assertThat(stockItem.canBePicked()).isFalse();
    assertThat(stockItem.getDomainEvents()).hasSize(2); // StockExpiredEvent + StockClassifiedEvent
}
```

### Integration Tests - Scheduled Job

```java
@SpringBootTest
@TestPropertySource(properties = {
    "stock.expiration.check.enabled=false" // Disable scheduled execution
})
class ExpirationCheckSchedulerIntegrationTest {

    @Autowired
    private ExpirationCheckScheduler scheduler;

    @Autowired
    private StockItemRepository stockItemRepository;

    @Test
    void shouldCheckAllStockItemsAndGenerateAlerts() {
        // Given: Stock items with various expiration dates
        createStockItemWithExpiration(LocalDate.now().plusDays(5)); // CRITICAL
        createStockItemWithExpiration(LocalDate.now().plusDays(20)); // NEAR_EXPIRY
        createStockItemWithExpiration(LocalDate.now().minusDays(1)); // EXPIRED

        // When: Run scheduled job
        scheduler.checkExpirationDates();

        // Then: Verify classifications updated
        List<StockItem> criticalStock = stockItemRepository
                .findByClassification(StockClassification.CRITICAL);
        assertThat(criticalStock).hasSize(1);

        List<StockItem> expiredStock = stockItemRepository
                .findByClassification(StockClassification.EXPIRED);
        assertThat(expiredStock).hasSize(1);
    }
}
```

---

## Implementation Checklist

### Frontend
- [ ] Create `ExpiringStockDashboard` page
- [ ] Create `ExpirationAlertCard` component
- [ ] Create `ExpiringStockList` component
- [ ] Create `useExpiringStock` hook
- [ ] Add routing for expiring stock dashboard
- [ ] Test UI with mock data

### Backend - Domain Core
- [ ] Add `checkExpiration()` method to `StockItem`
- [ ] Create `StockClassification` enum
- [ ] Create `StockExpiringAlertEvent`
- [ ] Create `StockExpiredEvent`
- [ ] Write unit tests for expiration logic

### Backend - Scheduled Job
- [ ] Create `ExpirationCheckScheduler`
- [ ] Create `CheckExpirationDatesCommandHandler`
- [ ] Configure cron schedule
- [ ] Add configuration properties
- [ ] Test scheduled job execution

### Backend - Query
- [ ] Create `GetExpiringStockQuery`
- [ ] Create `GetExpiringStockQueryHandler`
- [ ] Add query endpoint in controller
- [ ] Create query DTOs
- [ ] Write integration tests

### Notification Integration
- [ ] Create `StockExpirationEventListener` in Notification Service
- [ ] Handle `StockExpiringAlertEvent`
- [ ] Handle `StockExpiredEvent`
- [ ] Create notifications with appropriate priority
- [ ] Test event consumption

### Gateway API Tests
- [ ] Test expiring stock query endpoint
- [ ] Test filtering by classification
- [ ] Test date range filtering
- [ ] Verify scheduled job runs correctly

### Documentation
- [ ] Document expiration checking process
- [ ] Document classification levels
- [ ] Update API documentation
- [ ] Create user guide for dashboard

---

**Document Version:** 1.0  
**Last Updated:** 2026-01-08  
**Status:** Ready for Implementation
