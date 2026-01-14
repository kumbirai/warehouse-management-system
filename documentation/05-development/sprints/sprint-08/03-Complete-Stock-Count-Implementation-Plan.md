# Complete Stock Count Implementation Plan

## US-8.1.3: Complete Stock Count

**Service:** Reconciliation Service
**Priority:** Must Have
**Story Points:** 5
**Sprint:** Sprint 8

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
9. [Implementation Checklist](#implementation-checklist)

---

## Overview

### User Story

**As a** warehouse supervisor
**I want** to complete stock counts with automatic variance calculation and severity classification
**So that** I can identify discrepancies and route them for investigation

### Business Requirements

- **Automatic variance calculation** for all counted entries (counted - system quantity)
- **Variance percentage calculation** to identify proportional discrepancies
- **Severity classification** (LOW, MEDIUM, HIGH, CRITICAL) based on configurable thresholds
- **Review page** with variance highlighting and color coding
- **Completion confirmation** with validation checks
- **Critical variance prevention** - block completion if critical variances unresolved
- **Summary statistics** - total entries, variance counts by severity, financial impact
- **Domain events** - publish completion and variance identification events

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Multi-tenant support with schema isolation
- Move common value objects to `common-domain` (DRY principle)
- Configurable variance thresholds (percentage and monetary value)
- Comprehensive validation before completion
- Audit trail with completion timestamp and operator

### Acceptance Criteria

1. **AC-1**: System calculates variances (counted - system quantity)
2. **AC-2**: System classifies variance severity (LOW, MEDIUM, HIGH, CRITICAL) based on thresholds
3. **AC-3**: System displays variance summary with color coding
4. **AC-4**: System requires review confirmation before completion
5. **AC-5**: System publishes `StockCountCompletedEvent`
6. **AC-6**: System publishes `StockCountVarianceIdentifiedEvent` for significant variances
7. **AC-7**: System prevents completion if critical variances are unresolved

### Key Features

#### Variance Calculation

- **Quantity Variance**: `counted_quantity - system_quantity`
- **Variance Percentage**: `((counted - system) / system) * 100`
- **Absolute Value**: `|variance_quantity| * unit_price`
- **Financial Impact**: Sum of all absolute variance values

#### Severity Classification

Dual-threshold approach (percentage AND value):

| Severity | Percentage Threshold | Value Threshold | Action Required |
|----------|---------------------|-----------------|-----------------|
| LOW      | 0-5%                | R 0-100         | Informational   |
| MEDIUM   | 5-10%               | R 100-500       | Investigation   |
| HIGH     | 10-20%              | R 500-1000      | Investigation + Approval |
| CRITICAL | >20%                | >R 1000         | Blocked until resolved |

---

## UI Design

### Stock Count Completion Page

**Component:** `StockCountCompletionPage.tsx`
**Route:** `/reconciliation/stock-count/:id/complete`

#### Component Hierarchy

```
StockCountCompletionPage
├── PageHeader
│   ├── StockCountTitle (Count reference, date)
│   └── StatusBadge (IN_PROGRESS → COMPLETED)
├── CompletionSteps
│   ├── Step1: ReviewAllEntries
│   ├── Step2: ReviewVariances
│   └── Step3: ConfirmCompletion
├── EntryReviewSection
│   ├── StockCountSummaryCard
│   │   ├── TotalEntriesMetric
│   │   ├── TotalCountedQuantityMetric
│   │   └── CompletionPercentageMetric
│   └── EntryDataTable (All entries with variance column)
│       ├── LocationColumn
│       ├── ProductColumn
│       ├── SystemQuantityColumn
│       ├── CountedQuantityColumn
│       └── VarianceColumn (Color-coded)
├── VarianceReviewSection
│   ├── VarianceSummaryCards
│   │   ├── CriticalVariancesCard (Red alert)
│   │   ├── HighVariancesCard (Orange warning)
│   │   ├── MediumVariancesCard (Yellow caution)
│   │   └── LowVariancesCard (Gray info)
│   ├── VarianceBreakdownChart (Pie chart by severity)
│   ├── FinancialImpactSummary
│   │   ├── TotalVarianceValue
│   │   ├── PositiveVarianceValue (Overage)
│   │   └── NegativeVarianceValue (Shortage)
│   └── VarianceDetailsTable
│       └── VarianceRow[]
│           ├── ProductInfo
│           ├── VarianceAmount (quantity and percentage)
│           ├── SeverityBadge
│           ├── FinancialImpact
│           └── ActionButton (Investigate)
├── ValidationSection
│   ├── PreCompletionChecklist
│   │   ├── AllEntriesRecorded (✓/✗)
│   │   ├── NoCriticalVariancesUnresolved (✓/✗)
│   │   └── NoSystemErrors (✓/✗)
│   └── ValidationWarnings[]
└── ActionButtons
    ├── SaveDraftButton
    ├── CompleteStockCountButton (Disabled if validation fails)
    └── CancelButton
```

#### TypeScript Types

```typescript
export enum VarianceSeverity {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export interface StockCountEntry {
  entryId: string;
  locationId: string;
  locationCode: string;
  productId: string;
  productCode: string;
  productDescription: string;
  systemQuantity: number;
  countedQuantity: number;
  varianceQuantity: number;      // counted - system
  variancePercentage: number;    // ((counted - system) / system) * 100
}

export interface StockCountVariance {
  varianceId: string;
  entryId: string;
  locationCode: string;
  productCode: string;
  productDescription: string;
  systemQuantity: number;
  countedQuantity: number;
  varianceQuantity: number;
  variancePercentage: number;
  absoluteVarianceValue: number; // |variance| * unit_price
  severity: VarianceSeverity;
  investigationStatus: InvestigationStatus;
}

export interface VarianceSummary {
  totalVariances: number;
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  totalFinancialImpact: number;
  positiveVarianceValue: number; // Overage
  negativeVarianceValue: number; // Shortage
}

export interface CompleteStockCountRequest {
  stockCountId: string;
  completionNotes?: string;
  acknowledgeVariances: boolean;
}

export interface CompleteStockCountResponse {
  stockCountId: string;
  status: StockCountStatus;
  completedAt: string;
  completedBy: string;
  totalEntries: number;
  totalVariances: number;
  varianceSummary: VarianceSummary;
  criticalVariancesBlocked: boolean;
}
```

#### Key UI Components

##### 1. Variance Summary Cards

```typescript
interface VarianceSummaryCardsProps {
  summary: VarianceSummary;
  onViewDetails: (severity: VarianceSeverity) => void;
}

const VarianceSummaryCards: React.FC<VarianceSummaryCardsProps> = ({
  summary,
  onViewDetails,
}) => {
  return (
    <Grid container spacing={3}>
      {/* CRITICAL Variances */}
      <Grid item xs={12} md={3}>
        <Card
          sx={{
            borderLeft: 6,
            borderColor: 'error.main',
            backgroundColor: summary.criticalCount > 0 ? 'error.light' : undefined
          }}
        >
          <CardContent>
            <Box display="flex" alignItems="center" justifyContent="space-between">
              <Box>
                <Typography variant="h4" color="error">
                  {summary.criticalCount}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  CRITICAL Variances
                </Typography>
              </Box>
              <ErrorIcon color="error" sx={{ fontSize: 40 }} />
            </Box>
            {summary.criticalCount > 0 && (
              <Alert severity="error" sx={{ mt: 2 }}>
                Completion blocked until resolved
              </Alert>
            )}
            <Button
              fullWidth
              size="small"
              onClick={() => onViewDetails(VarianceSeverity.CRITICAL)}
              sx={{ mt: 1 }}
            >
              View Details
            </Button>
          </CardContent>
        </Card>
      </Grid>

      {/* HIGH Variances */}
      <Grid item xs={12} md={3}>
        <Card sx={{ borderLeft: 6, borderColor: 'warning.main' }}>
          <CardContent>
            <Box display="flex" alignItems="center" justifyContent="space-between">
              <Box>
                <Typography variant="h4" color="warning.main">
                  {summary.highCount}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  HIGH Variances
                </Typography>
              </Box>
              <WarningIcon color="warning" sx={{ fontSize: 40 }} />
            </Box>
            <Button
              fullWidth
              size="small"
              onClick={() => onViewDetails(VarianceSeverity.HIGH)}
              sx={{ mt: 1 }}
            >
              View Details
            </Button>
          </CardContent>
        </Card>
      </Grid>

      {/* MEDIUM Variances */}
      <Grid item xs={12} md={3}>
        <Card sx={{ borderLeft: 6, borderColor: 'info.main' }}>
          <CardContent>
            <Box display="flex" alignItems="center" justifyContent="space-between">
              <Box>
                <Typography variant="h4" color="info.main">
                  {summary.mediumCount}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  MEDIUM Variances
                </Typography>
              </Box>
              <InfoIcon color="info" sx={{ fontSize: 40 }} />
            </Box>
            <Button
              fullWidth
              size="small"
              onClick={() => onViewDetails(VarianceSeverity.MEDIUM)}
              sx={{ mt: 1 }}
            >
              View Details
            </Button>
          </CardContent>
        </Card>
      </Grid>

      {/* LOW Variances */}
      <Grid item xs={12} md={3}>
        <Card sx={{ borderLeft: 6, borderColor: 'grey.400' }}>
          <CardContent>
            <Box display="flex" alignItems="center" justifyContent="space-between">
              <Box>
                <Typography variant="h4" color="text.secondary">
                  {summary.lowCount}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  LOW Variances
                </Typography>
              </Box>
              <CheckCircleIcon color="disabled" sx={{ fontSize: 40 }} />
            </Box>
            <Button
              fullWidth
              size="small"
              onClick={() => onViewDetails(VarianceSeverity.LOW)}
              sx={{ mt: 1 }}
            >
              View Details
            </Button>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );
};
```

##### 2. Financial Impact Summary

```typescript
interface FinancialImpactSummaryProps {
  summary: VarianceSummary;
}

const FinancialImpactSummary: React.FC<FinancialImpactSummaryProps> = ({ summary }) => {
  return (
    <Card>
      <CardHeader title="Financial Impact Analysis" />
      <CardContent>
        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <Box textAlign="center">
              <Typography variant="h5" color={summary.totalFinancialImpact > 0 ? 'error' : 'success'}>
                R {Math.abs(summary.totalFinancialImpact).toFixed(2)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Total Financial Impact
              </Typography>
              <Chip
                label={summary.totalFinancialImpact > 0 ? 'Net Shortage' : 'Net Overage'}
                color={summary.totalFinancialImpact > 0 ? 'error' : 'success'}
                size="small"
                sx={{ mt: 1 }}
              />
            </Box>
          </Grid>

          <Grid item xs={12} md={4}>
            <Box textAlign="center">
              <Typography variant="h5" color="success.main">
                R {summary.positiveVarianceValue.toFixed(2)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Overage (Counted &gt; System)
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Excess inventory found
              </Typography>
            </Box>
          </Grid>

          <Grid item xs={12} md={4}>
            <Box textAlign="center">
              <Typography variant="h5" color="error">
                R {Math.abs(summary.negativeVarianceValue).toFixed(2)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Shortage (Counted &lt; System)
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Missing inventory
              </Typography>
            </Box>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
};
```

##### 3. Variance Details Table

```typescript
interface VarianceDetailsTableProps {
  variances: StockCountVariance[];
  onInvestigate: (varianceId: string) => void;
}

const VarianceDetailsTable: React.FC<VarianceDetailsTableProps> = ({
  variances,
  onInvestigate,
}) => {
  const getSeverityColor = (severity: VarianceSeverity): string => {
    switch (severity) {
      case VarianceSeverity.CRITICAL:
        return 'error';
      case VarianceSeverity.HIGH:
        return 'warning';
      case VarianceSeverity.MEDIUM:
        return 'info';
      case VarianceSeverity.LOW:
        return 'default';
    }
  };

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Location</TableCell>
            <TableCell>Product</TableCell>
            <TableCell align="right">System Qty</TableCell>
            <TableCell align="right">Counted Qty</TableCell>
            <TableCell align="right">Variance</TableCell>
            <TableCell align="right">Variance %</TableCell>
            <TableCell>Severity</TableCell>
            <TableCell align="right">Financial Impact</TableCell>
            <TableCell align="center">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {variances.map((variance) => (
            <TableRow
              key={variance.varianceId}
              sx={{
                backgroundColor: variance.severity === VarianceSeverity.CRITICAL
                  ? 'error.light'
                  : undefined,
              }}
            >
              <TableCell>{variance.locationCode}</TableCell>
              <TableCell>
                <Typography variant="body2" fontWeight="bold">
                  {variance.productCode}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {variance.productDescription}
                </Typography>
              </TableCell>
              <TableCell align="right">{variance.systemQuantity.toFixed(2)}</TableCell>
              <TableCell align="right">{variance.countedQuantity.toFixed(2)}</TableCell>
              <TableCell align="right">
                <Typography
                  variant="body2"
                  fontWeight="bold"
                  color={variance.varianceQuantity > 0 ? 'success.main' : 'error'}
                >
                  {variance.varianceQuantity > 0 ? '+' : ''}
                  {variance.varianceQuantity.toFixed(2)}
                </Typography>
              </TableCell>
              <TableCell align="right">
                <Typography
                  variant="body2"
                  fontWeight="bold"
                  color={variance.variancePercentage > 0 ? 'success.main' : 'error'}
                >
                  {variance.variancePercentage > 0 ? '+' : ''}
                  {variance.variancePercentage.toFixed(2)}%
                </Typography>
              </TableCell>
              <TableCell>
                <Chip
                  label={variance.severity}
                  color={getSeverityColor(variance.severity) as any}
                  size="small"
                />
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2" fontWeight="bold">
                  R {variance.absoluteVarianceValue.toFixed(2)}
                </Typography>
              </TableCell>
              <TableCell align="center">
                <IconButton
                  size="small"
                  color="primary"
                  onClick={() => onInvestigate(variance.varianceId)}
                >
                  <SearchIcon />
                </IconButton>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
```

##### 4. Pre-Completion Validation Checklist

```typescript
interface PreCompletionChecklistProps {
  stockCountId: string;
  totalEntries: number;
  totalWorksheetEntries: number;
  criticalVarianceCount: number;
  systemErrors: string[];
}

const PreCompletionChecklist: React.FC<PreCompletionChecklistProps> = ({
  stockCountId,
  totalEntries,
  totalWorksheetEntries,
  criticalVarianceCount,
  systemErrors,
}) => {
  const allEntriesRecorded = totalEntries === totalWorksheetEntries;
  const noCriticalVariances = criticalVarianceCount === 0;
  const noSystemErrors = systemErrors.length === 0;

  const canComplete = allEntriesRecorded && noCriticalVariances && noSystemErrors;

  return (
    <Card>
      <CardHeader title="Pre-Completion Validation" />
      <CardContent>
        <List>
          {/* All Entries Recorded */}
          <ListItem>
            <ListItemIcon>
              {allEntriesRecorded ? (
                <CheckCircleIcon color="success" />
              ) : (
                <ErrorIcon color="error" />
              )}
            </ListItemIcon>
            <ListItemText
              primary="All Entries Recorded"
              secondary={`${totalEntries} of ${totalWorksheetEntries} products counted`}
            />
          </ListItem>

          {/* No Critical Variances */}
          <ListItem>
            <ListItemIcon>
              {noCriticalVariances ? (
                <CheckCircleIcon color="success" />
              ) : (
                <ErrorIcon color="error" />
              )}
            </ListItemIcon>
            <ListItemText
              primary="No Unresolved Critical Variances"
              secondary={
                criticalVarianceCount > 0
                  ? `${criticalVarianceCount} critical variance(s) must be resolved`
                  : 'All critical variances resolved'
              }
            />
          </ListItem>

          {/* No System Errors */}
          <ListItem>
            <ListItemIcon>
              {noSystemErrors ? (
                <CheckCircleIcon color="success" />
              ) : (
                <ErrorIcon color="error" />
              )}
            </ListItemIcon>
            <ListItemText
              primary="No System Errors"
              secondary={
                systemErrors.length > 0
                  ? `${systemErrors.length} error(s) detected`
                  : 'All validations passed'
              }
            />
          </ListItem>
        </List>

        {!canComplete && (
          <Alert severity="warning" sx={{ mt: 2 }}>
            Stock count cannot be completed until all validation checks pass.
          </Alert>
        )}

        {systemErrors.length > 0 && (
          <Box sx={{ mt: 2 }}>
            <Typography variant="subtitle2" gutterBottom>
              System Errors:
            </Typography>
            {systemErrors.map((error, index) => (
              <Alert key={index} severity="error" sx={{ mb: 1 }}>
                {error}
              </Alert>
            ))}
          </Box>
        )}
      </CardContent>
    </Card>
  );
};
```

---

## Domain Model Design

### Domain Service

#### VarianceCalculationService

**Location:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/service/VarianceCalculationService.java`

**Responsibility:** Domain service for calculating and classifying variances

```java
package com.ccbsa.wms.reconciliation.domain.core.service;

import com.ccbsa.common.domain.valueobject.Money;
import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.reconciliation.domain.core.entity.StockCountEntry;
import com.ccbsa.wms.reconciliation.domain.core.entity.StockCountVariance;
import com.ccbsa.wms.reconciliation.domain.core.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class VarianceCalculationService {

    private final VarianceThresholdConfig thresholdConfig;

    /**
     * Calculate variances for all stock count entries
     */
    public List<StockCountVariance> calculateVariances(
            StockCountId stockCountId,
            List<StockCountEntry> entries,
            ProductPriceProvider priceProvider) {

        return entries.stream()
            .filter(this::hasVariance)
            .map(entry -> calculateVarianceForEntry(stockCountId, entry, priceProvider))
            .collect(Collectors.toList());
    }

    /**
     * Calculate variance for a single entry
     */
    private StockCountVariance calculateVarianceForEntry(
            StockCountId stockCountId,
            StockCountEntry entry,
            ProductPriceProvider priceProvider) {

        // Calculate variance quantity
        Quantity varianceQuantity = calculateVarianceQuantity(
            entry.getSystemQuantity(),
            entry.getCountedQuantity()
        );

        // Calculate variance percentage
        BigDecimal variancePercentage = calculateVariancePercentage(
            entry.getSystemQuantity(),
            entry.getCountedQuantity()
        );

        // Get product unit price
        Money unitPrice = priceProvider.getUnitPrice(entry.getProductId());

        // Calculate absolute variance value
        Money absoluteVarianceValue = calculateAbsoluteVarianceValue(
            varianceQuantity,
            unitPrice
        );

        // Classify severity
        VarianceSeverity severity = classifyVarianceSeverity(
            variancePercentage,
            absoluteVarianceValue
        );

        // Create variance entity
        return StockCountVariance.create(
            StockCountVarianceId.generate(),
            stockCountId,
            entry.getEntryId(),
            entry.getLocationId(),
            entry.getProductId(),
            varianceQuantity,
            variancePercentage,
            absoluteVarianceValue,
            severity
        );
    }

    /**
     * Calculate variance quantity: counted - system
     */
    private Quantity calculateVarianceQuantity(Quantity systemQty, Quantity countedQty) {
        BigDecimal variance = countedQty.getValue().subtract(systemQty.getValue());
        return new Quantity(variance);
    }

    /**
     * Calculate variance percentage: ((counted - system) / system) * 100
     */
    private BigDecimal calculateVariancePercentage(Quantity systemQty, Quantity countedQty) {
        if (systemQty.getValue().compareTo(BigDecimal.ZERO) == 0) {
            // Handle division by zero: if system is 0 and counted > 0, return 100%
            if (countedQty.getValue().compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(100.00);
            }
            return BigDecimal.ZERO;
        }

        BigDecimal variance = countedQty.getValue().subtract(systemQty.getValue());
        BigDecimal percentage = variance
            .divide(systemQty.getValue(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);

        return percentage.abs(); // Return absolute value for percentage
    }

    /**
     * Calculate absolute variance value: |variance_quantity| * unit_price
     */
    private Money calculateAbsoluteVarianceValue(Quantity varianceQty, Money unitPrice) {
        BigDecimal absVariance = varianceQty.getValue().abs();
        BigDecimal value = absVariance.multiply(unitPrice.getAmount());
        return new Money(value);
    }

    /**
     * Classify variance severity using dual threshold (percentage AND value)
     */
    public VarianceSeverity classifyVarianceSeverity(
            BigDecimal variancePercentage,
            Money absoluteVarianceValue) {

        BigDecimal absPercentage = variancePercentage.abs();
        BigDecimal absValue = absoluteVarianceValue.getAmount();

        // CRITICAL: Either condition exceeds critical threshold
        if (absPercentage.compareTo(thresholdConfig.getCriticalPercentage()) > 0 ||
            absValue.compareTo(thresholdConfig.getCriticalValue()) > 0) {
            return VarianceSeverity.CRITICAL;
        }

        // HIGH: Either condition exceeds high threshold
        if (absPercentage.compareTo(thresholdConfig.getHighPercentage()) > 0 ||
            absValue.compareTo(thresholdConfig.getHighValue()) > 0) {
            return VarianceSeverity.HIGH;
        }

        // MEDIUM: Either condition exceeds medium threshold
        if (absPercentage.compareTo(thresholdConfig.getMediumPercentage()) > 0 ||
            absValue.compareTo(thresholdConfig.getMediumValue()) > 0) {
            return VarianceSeverity.MEDIUM;
        }

        // LOW: Everything else
        return VarianceSeverity.LOW;
    }

    /**
     * Calculate variance summary statistics
     */
    public VarianceSummary calculateVarianceSummary(List<StockCountVariance> variances) {
        int totalVariances = variances.size();
        int criticalCount = (int) variances.stream()
            .filter(v -> v.getSeverity() == VarianceSeverity.CRITICAL)
            .count();
        int highCount = (int) variances.stream()
            .filter(v -> v.getSeverity() == VarianceSeverity.HIGH)
            .count();
        int mediumCount = (int) variances.stream()
            .filter(v -> v.getSeverity() == VarianceSeverity.MEDIUM)
            .count();
        int lowCount = (int) variances.stream()
            .filter(v -> v.getSeverity() == VarianceSeverity.LOW)
            .count();

        // Calculate financial impact
        BigDecimal positiveVarianceValue = variances.stream()
            .filter(v -> v.getVarianceQuantity().getValue().compareTo(BigDecimal.ZERO) > 0)
            .map(v -> v.getAbsoluteVarianceValue().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal negativeVarianceValue = variances.stream()
            .filter(v -> v.getVarianceQuantity().getValue().compareTo(BigDecimal.ZERO) < 0)
            .map(v -> v.getAbsoluteVarianceValue().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .negate(); // Make negative

        BigDecimal totalFinancialImpact = positiveVarianceValue.add(negativeVarianceValue);

        return VarianceSummary.builder()
            .totalVariances(totalVariances)
            .criticalCount(criticalCount)
            .highCount(highCount)
            .mediumCount(mediumCount)
            .lowCount(lowCount)
            .totalFinancialImpact(new Money(totalFinancialImpact))
            .positiveVarianceValue(new Money(positiveVarianceValue))
            .negativeVarianceValue(new Money(negativeVarianceValue))
            .build();
    }

    /**
     * Check if entry has variance
     */
    private boolean hasVariance(StockCountEntry entry) {
        return entry.getCountedQuantity().getValue()
            .compareTo(entry.getSystemQuantity().getValue()) != 0;
    }
}
```

### Value Objects

#### VarianceSeverity Enum

**Location:** `common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/VarianceSeverity.java`

```java
package com.ccbsa.common.domain.valueobject;

public enum VarianceSeverity {
    LOW("Low", "Minor variance within acceptable tolerance", false, false),
    MEDIUM("Medium", "Moderate variance requiring investigation", true, false),
    HIGH("High", "Significant variance requiring investigation and approval", true, true),
    CRITICAL("Critical", "Critical variance blocking completion until resolved", true, true);

    private final String displayName;
    private final String description;
    private final boolean requiresInvestigation;
    private final boolean requiresApproval;

    VarianceSeverity(String displayName, String description,
                     boolean requiresInvestigation, boolean requiresApproval) {
        this.displayName = displayName;
        this.description = description;
        this.requiresInvestigation = requiresInvestigation;
        this.requiresApproval = requiresApproval;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresInvestigation() {
        return requiresInvestigation;
    }

    public boolean requiresApproval() {
        return requiresApproval;
    }

    public boolean blocksCompletion() {
        return this == CRITICAL;
    }
}
```

#### VarianceThresholdConfig

```java
package com.ccbsa.wms.reconciliation.domain.core.valueobject;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class VarianceThresholdConfig {
    private final BigDecimal lowPercentage;      // 5.0%
    private final BigDecimal mediumPercentage;   // 10.0%
    private final BigDecimal highPercentage;     // 20.0%
    private final BigDecimal criticalPercentage; // >20.0%

    private final BigDecimal lowValue;           // R 100
    private final BigDecimal mediumValue;        // R 500
    private final BigDecimal highValue;          // R 1000
    private final BigDecimal criticalValue;      // R 1000+

    public static VarianceThresholdConfig defaultConfig() {
        return VarianceThresholdConfig.builder()
            .lowPercentage(BigDecimal.valueOf(5.0))
            .mediumPercentage(BigDecimal.valueOf(10.0))
            .highPercentage(BigDecimal.valueOf(20.0))
            .criticalPercentage(BigDecimal.valueOf(20.0))
            .lowValue(BigDecimal.valueOf(100.00))
            .mediumValue(BigDecimal.valueOf(500.00))
            .highValue(BigDecimal.valueOf(1000.00))
            .criticalValue(BigDecimal.valueOf(1000.00))
            .build();
    }
}
```

#### VarianceSummary

```java
package com.ccbsa.wms.reconciliation.domain.core.valueobject;

import com.ccbsa.common.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VarianceSummary {
    private final int totalVariances;
    private final int criticalCount;
    private final int highCount;
    private final int mediumCount;
    private final int lowCount;
    private final Money totalFinancialImpact;
    private final Money positiveVarianceValue;   // Overage
    private final Money negativeVarianceValue;   // Shortage

    public boolean hasCriticalVariances() {
        return criticalCount > 0;
    }

    public boolean hasSignificantVariances() {
        return criticalCount > 0 || highCount > 0;
    }
}
```

### Aggregate Updates

#### StockCount.complete()

**Location:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/entity/StockCount.java`

```java
/**
 * Complete the stock count with variance calculation
 */
public void complete(
        List<StockCountVariance> variances,
        VarianceSummary varianceSummary,
        String completedBy) {

    validateCanBeCompleted();
    validateNoCriticalVariancesUnresolved(varianceSummary);

    this.variances = variances;
    this.varianceSummary = varianceSummary;
    this.status = StockCountStatus.COMPLETED;
    this.completedAt = ZonedDateTime.now();
    this.auditInfo = this.auditInfo.update(completedBy);

    // Publish stock count completed event
    registerEvent(new StockCountCompletedEvent(
        this.stockCountId,
        this.tenantId,
        this.totalEntries,
        varianceSummary.getTotalVariances(),
        varianceSummary.hasCriticalVariances(),
        ZonedDateTime.now()
    ));

    // Publish variance identified events for significant variances
    variances.stream()
        .filter(v -> v.getSeverity().requiresInvestigation())
        .forEach(variance -> registerEvent(new StockCountVarianceIdentifiedEvent(
            variance.getVarianceId(),
            this.stockCountId,
            this.tenantId,
            variance.getProductId(),
            variance.getVarianceQuantity(),
            variance.getSeverity(),
            ZonedDateTime.now()
        )));
}

private void validateCanBeCompleted() {
    if (this.status == StockCountStatus.COMPLETED) {
        throw new InvalidStockCountException(
            "Stock count is already completed"
        );
    }

    if (this.status == StockCountStatus.CANCELLED) {
        throw new InvalidStockCountException(
            "Cannot complete a cancelled stock count"
        );
    }

    if (this.totalEntries == 0) {
        throw new InvalidStockCountException(
            "Cannot complete stock count with no entries"
        );
    }
}

private void validateNoCriticalVariancesUnresolved(VarianceSummary summary) {
    if (summary.hasCriticalVariances()) {
        throw new CriticalVarianceBlockException(
            String.format("Cannot complete stock count. %d critical variance(s) must be resolved first",
                summary.getCriticalCount())
        );
    }
}
```

---

## Backend Implementation

### Command

#### CompleteStockCountCommand

**Location:** `services/reconciliation-service/reconciliation-domain/reconciliation-application-service/src/main/java/com/ccbsa/wms/reconciliation/application/service/command/dto/CompleteStockCountCommand.java`

```java
package com.ccbsa.wms.reconciliation.application.service.command.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompleteStockCountCommand {
    private final String stockCountId;
    private final String completionNotes;
    private final boolean acknowledgeVariances;
    private final String tenantId;
    private final String completedBy;
}
```

### Command Handler

```java
package com.ccbsa.wms.reconciliation.application.service.command;

import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.reconciliation.application.service.command.dto.*;
import com.ccbsa.wms.reconciliation.application.service.port.repository.*;
import com.ccbsa.wms.reconciliation.application.service.port.service.ProductPriceProvider;
import com.ccbsa.wms.reconciliation.domain.core.entity.*;
import com.ccbsa.wms.reconciliation.domain.core.service.VarianceCalculationService;
import com.ccbsa.wms.reconciliation.domain.core.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompleteStockCountCommandHandler {

    private final StockCountRepository stockCountRepository;
    private final StockCountVarianceRepository stockCountVarianceRepository;
    private final VarianceCalculationService varianceCalculationService;
    private final ProductPriceProvider productPriceProvider;

    @Transactional
    public CompleteStockCountResult handle(CompleteStockCountCommand command) {
        log.info("Completing stock count: {}", command.getStockCountId());

        StockCountId stockCountId = StockCountId.of(command.getStockCountId());
        TenantId tenantId = TenantId.of(command.getTenantId());

        // Retrieve stock count
        StockCount stockCount = stockCountRepository.findById(stockCountId, tenantId)
            .orElseThrow(() -> new StockCountNotFoundException(
                String.format("Stock count not found: %s", command.getStockCountId())
            ));

        // Retrieve all entries
        List<StockCountEntry> entries = stockCount.getEntries();

        // Calculate variances
        List<StockCountVariance> variances = varianceCalculationService.calculateVariances(
            stockCountId,
            entries,
            productPriceProvider
        );

        // Calculate variance summary
        VarianceSummary varianceSummary = varianceCalculationService.calculateVarianceSummary(variances);

        log.info("Stock count {} has {} variances. Breakdown: CRITICAL={}, HIGH={}, MEDIUM={}, LOW={}",
            stockCountId.getValue(),
            varianceSummary.getTotalVariances(),
            varianceSummary.getCriticalCount(),
            varianceSummary.getHighCount(),
            varianceSummary.getMediumCount(),
            varianceSummary.getLowCount()
        );

        // Validate acknowledgement for variances
        if (varianceSummary.getTotalVariances() > 0 && !command.isAcknowledgeVariances()) {
            throw new VarianceAcknowledgementRequiredException(
                "Variances detected. User must acknowledge before completion"
            );
        }

        // Complete stock count (will validate no critical variances)
        stockCount.complete(variances, varianceSummary, command.getCompletedBy());

        // Save stock count
        StockCount completedStockCount = stockCountRepository.save(stockCount);

        // Save variances
        variances.forEach(stockCountVarianceRepository::save);

        log.info("Stock count completed successfully. ID: {}, Total Variances: {}, Financial Impact: R {}",
            completedStockCount.getStockCountId().getValue(),
            varianceSummary.getTotalVariances(),
            varianceSummary.getTotalFinancialImpact().getAmount()
        );

        return mapToResult(completedStockCount, varianceSummary);
    }

    private CompleteStockCountResult mapToResult(StockCount stockCount, VarianceSummary summary) {
        return CompleteStockCountResult.builder()
            .stockCountId(stockCount.getStockCountId().getValue())
            .status(stockCount.getStatus())
            .completedAt(stockCount.getCompletedAt())
            .completedBy(stockCount.getAuditInfo().getUpdatedBy())
            .totalEntries(stockCount.getTotalEntries())
            .totalVariances(summary.getTotalVariances())
            .varianceSummary(summary)
            .criticalVariancesBlocked(summary.hasCriticalVariances())
            .build();
    }
}
```

---

## Frontend Implementation

### React Hook

```typescript
// useCompleteStockCount.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { stockCountService } from '../services/stockCountService';
import type { CompleteStockCountRequest, CompleteStockCountResponse } from '../types/stockCount';

export const useCompleteStockCount = () => {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation<CompleteStockCountResponse, Error, CompleteStockCountRequest>({
    mutationFn: (request) => stockCountService.completeStockCount(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['stock-counts'] });
      queryClient.invalidateQueries({ queryKey: ['stock-count', data.stockCountId] });

      if (data.totalVariances > 0) {
        enqueueSnackbar(
          `Stock count completed with ${data.totalVariances} variance(s)`,
          { variant: 'warning' }
        );
      } else {
        enqueueSnackbar('Stock count completed successfully - no variances', {
          variant: 'success',
        });
      }
    },
    onError: (error) => {
      if (error.message.includes('critical variance')) {
        enqueueSnackbar('Cannot complete: Critical variances must be resolved first', {
          variant: 'error',
        });
      } else {
        enqueueSnackbar(`Failed to complete stock count: ${error.message}`, {
          variant: 'error',
        });
      }
    },
  });
};
```

### Service Method

```typescript
// stockCountService.ts
async completeStockCount(
  request: CompleteStockCountRequest
): Promise<CompleteStockCountResponse> {
  const response = await apiClient.post<CompleteStockCountResponse>(
    `/api/reconciliation/stock-counts/${request.stockCountId}/complete`,
    {
      completionNotes: request.completionNotes,
      acknowledgeVariances: request.acknowledgeVariances,
    }
  );
  return response.data;
}
```

---

## Data Flow

```
StockCountCompletionPage
  ↓ User clicks "Complete Stock Count"
  ↓ POST /api/reconciliation/stock-counts/{id}/complete
Gateway Service
  ↓ Route to Reconciliation Service
Reconciliation Service (Command Controller)
  ↓ CompleteStockCountCommand
Command Handler
  ↓ Load StockCount + Entries
  ↓ Calculate Variances (VarianceCalculationService)
  ↓ Calculate Variance Summary
  ↓ Validate no CRITICAL variances unresolved
  ↓ StockCount.complete()
Domain Core (StockCount Aggregate)
  ↓ Publish StockCountCompletedEvent
  ↓ Publish StockCountVarianceIdentifiedEvent (for each significant variance)
Event Publisher
  ↓ Kafka Topic: reconciliation-events
Stock Management Service (Event Listener)
  ↓ Adjust stock levels based on counted quantities
Notification Service (Event Listener)
  ↓ Alert supervisors of critical variances
Response
  ↓ CompleteStockCountResponse with variance summary
Frontend
  ↓ Display completion confirmation
  ↓ Navigate to variance investigation if needed
```

---

## Testing Strategy

### Unit Tests

#### Domain Core Tests

```java
@Test
void completeStockCount_WithNoCriticalVariances_ShouldComplete() {
    // Arrange
    StockCount stockCount = createStockCountWithEntries();
    List<StockCountVariance> variances = createVariancesWithoutCritical();
    VarianceSummary summary = VarianceSummary.builder()
        .totalVariances(3)
        .criticalCount(0)
        .highCount(1)
        .mediumCount(2)
        .lowCount(0)
        .totalFinancialImpact(new Money(BigDecimal.valueOf(500)))
        .positiveVarianceValue(new Money(BigDecimal.valueOf(300)))
        .negativeVarianceValue(new Money(BigDecimal.valueOf(-200)))
        .build();

    // Act
    stockCount.complete(variances, summary, "supervisor1");

    // Assert
    assertThat(stockCount.getStatus()).isEqualTo(StockCountStatus.COMPLETED);
    assertThat(stockCount.getVariances()).hasSize(3);
    assertThat(stockCount.getDomainEvents()).hasSize(4); // 1 completed + 3 variance events
}

@Test
void completeStockCount_WithCriticalVariances_ShouldThrowException() {
    // Arrange
    StockCount stockCount = createStockCountWithEntries();
    List<StockCountVariance> variances = createVariancesWithCritical();
    VarianceSummary summary = VarianceSummary.builder()
        .totalVariances(2)
        .criticalCount(1)
        .highCount(0)
        .mediumCount(1)
        .lowCount(0)
        .totalFinancialImpact(new Money(BigDecimal.valueOf(2000)))
        .positiveVarianceValue(new Money(BigDecimal.ZERO))
        .negativeVarianceValue(new Money(BigDecimal.valueOf(-2000)))
        .build();

    // Act & Assert
    assertThatThrownBy(() -> stockCount.complete(variances, summary, "supervisor1"))
        .isInstanceOf(CriticalVarianceBlockException.class)
        .hasMessageContaining("1 critical variance(s) must be resolved");
}

@Test
void classifyVarianceSeverity_HighPercentageLowValue_ShouldReturnHigh() {
    // Arrange
    VarianceThresholdConfig config = VarianceThresholdConfig.defaultConfig();
    VarianceCalculationService service = new VarianceCalculationService(config);
    BigDecimal variancePercentage = BigDecimal.valueOf(15.0); // 15% > 10% HIGH threshold
    Money absoluteValue = new Money(BigDecimal.valueOf(50)); // < R100 LOW threshold

    // Act
    VarianceSeverity severity = service.classifyVarianceSeverity(
        variancePercentage,
        absoluteValue
    );

    // Assert - Either threshold triggers HIGH
    assertThat(severity).isEqualTo(VarianceSeverity.HIGH);
}

@Test
void calculateVariancePercentage_SystemQuantityZero_ShouldReturn100Percent() {
    // Arrange
    VarianceThresholdConfig config = VarianceThresholdConfig.defaultConfig();
    VarianceCalculationService service = new VarianceCalculationService(config);
    Quantity systemQty = new Quantity(BigDecimal.ZERO);
    Quantity countedQty = new Quantity(BigDecimal.TEN);

    // Act
    BigDecimal percentage = service.calculateVariancePercentage(systemQty, countedQty);

    // Assert
    assertThat(percentage).isEqualByComparingTo(BigDecimal.valueOf(100.00));
}
```

### Integration Tests

```java
@Test
void completeStockCount_EndToEnd_ShouldCompleteWithVariances() {
    // Arrange
    String stockCountId = createStockCountWithEntries();
    CompleteStockCountRequest request = CompleteStockCountRequest.builder()
        .stockCountId(stockCountId)
        .acknowledgeVariances(true)
        .completionNotes("Completion test")
        .build();

    // Act
    webTestClient.post()
        .uri("/api/reconciliation/stock-counts/{id}/complete", stockCountId)
        .header("X-Tenant-ID", testTenantId)
        .header("Authorization", "Bearer " + authToken)
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk()
        .expectBody(CompleteStockCountResponse.class)
        .value(response -> {
            assertThat(response.getStockCountId()).isEqualTo(stockCountId);
            assertThat(response.getStatus()).isEqualTo(StockCountStatus.COMPLETED);
            assertThat(response.getTotalVariances()).isGreaterThan(0);
            assertThat(response.getVarianceSummary()).isNotNull();
            assertThat(response.isCriticalVariancesBlocked()).isFalse();
        });
}
```

---

## Acceptance Criteria Validation

| # | Acceptance Criterion | Implementation Status | Validation |
|---|----------------------|----------------------|------------|
| 1 | System calculates variances (counted - system quantity) | ✅ Implemented | `VarianceCalculationService.calculateVarianceQuantity()` |
| 2 | System classifies variance severity (LOW, MEDIUM, HIGH, CRITICAL) based on thresholds | ✅ Implemented | `VarianceCalculationService.classifyVarianceSeverity()` with dual thresholds |
| 3 | System displays variance summary with color coding | ✅ Implemented | `VarianceSummaryCards` component with severity-based colors |
| 4 | System requires review confirmation before completion | ✅ Implemented | `acknowledgeVariances` flag in `CompleteStockCountRequest` |
| 5 | System publishes `StockCountCompletedEvent` | ✅ Implemented | Published in `StockCount.complete()` |
| 6 | System publishes `StockCountVarianceIdentifiedEvent` for significant variances | ✅ Implemented | Published for variances requiring investigation |
| 7 | System prevents completion if critical variances are unresolved | ✅ Implemented | `validateNoCriticalVariancesUnresolved()` throws `CriticalVarianceBlockException` |

---

## Implementation Checklist

### Common Module

- [ ] Create `VarianceSeverity` enum in `common-domain`
- [ ] Update cache namespaces for variances

### Reconciliation Service - Domain Core

- [ ] Create `VarianceCalculationService` domain service
- [ ] Create `VarianceThresholdConfig` value object
- [ ] Create `VarianceSummary` value object
- [ ] Create `ProductPriceProvider` port interface
- [ ] Update `StockCount` aggregate with `complete()` method
- [ ] Create `CriticalVarianceBlockException`
- [ ] Create `VarianceAcknowledgementRequiredException`
- [ ] Create `StockCountCompletedEvent`
- [ ] Create `StockCountVarianceIdentifiedEvent`
- [ ] Unit tests for variance calculation logic
- [ ] Unit tests for severity classification

### Reconciliation Service - Application Service

- [ ] Create `CompleteStockCountCommand`
- [ ] Create `CompleteStockCountResult`
- [ ] Create `CompleteStockCountCommandHandler`
- [ ] Create `StockCountVarianceRepository` port
- [ ] Unit tests for command handler

### Reconciliation Service - Data Access

- [ ] Create `StockCountVarianceEntity`
- [ ] Create `StockCountVarianceJpaRepository`
- [ ] Create `StockCountVarianceRepositoryAdapter`
- [ ] Create database migration for variance tables
- [ ] Create `ProductPriceProviderAdapter` (integrates with Product Service)

### Reconciliation Service - Application Layer

- [ ] Create `CompleteStockCountRequestDTO`
- [ ] Create `CompleteStockCountResponseDTO`
- [ ] Create `VarianceSummaryDTO`
- [ ] Update `StockCountCommandController` with complete endpoint
- [ ] Add validation annotations

### Reconciliation Service - Messaging

- [ ] Create `StockCountCompletedEventPublisher`
- [ ] Create `StockCountVarianceIdentifiedEventPublisher`
- [ ] Configure Kafka topics
- [ ] Integration tests for event publishing

### Stock Management Service

- [ ] Create `StockCountCompletedEventListener`
- [ ] Implement stock level adjustment logic
- [ ] Tests for stock adjustment

### Notification Service

- [ ] Create `StockCountVarianceIdentifiedEventListener`
- [ ] Send alerts for critical/high variances
- [ ] Tests for notification logic

### Frontend

- [ ] Create variance types
- [ ] Create `useCompleteStockCount` hook
- [ ] Create `VarianceSummaryCards` component
- [ ] Create `FinancialImpactSummary` component
- [ ] Create `VarianceDetailsTable` component
- [ ] Create `PreCompletionChecklist` component
- [ ] Create `StockCountCompletionPage`
- [ ] Add routing for completion page
- [ ] Integration tests

### Gateway API Tests

- [ ] Create complete stock count test
- [ ] Test variance calculation accuracy
- [ ] Test severity classification
- [ ] Test critical variance blocking
- [ ] Test event publishing

### Configuration

- [ ] Add variance threshold configuration to `application.yml`
- [ ] Document configuration properties
- [ ] Add environment-specific thresholds

---

**End of Implementation Plan**
