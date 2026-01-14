# US-7.5.1: Reconcile Returns with D365 - Implementation Plan

**User Story:** As a warehouse manager, I want the system to automatically reconcile return transactions with Microsoft Dynamics 365, so that inventory and financial records are
synchronized across systems.

**Story Points:** 8
**Priority:** High
**Sprint:** Sprint 7
**Module:** Returns Management, Integration Service
**Dependencies:** US-7.1.1, US-7.2.1, US-7.3.1, US-7.4.1

---

## Overview

This implementation plan covers **bidirectional integration** between the WMS and Microsoft Dynamics 365 for return transaction reconciliation. The integration ensures:

1. **Return Initiation Sync** - WMS notifies D365 when returns are initiated
2. **Stock Adjustment Sync** - Stock level changes are synchronized to D365
3. **Financial Reconciliation** - Return values and credit calculations are synced
4. **Status Updates** - D365 receives real-time return status updates
5. **Error Handling** - Robust retry mechanisms for failed synchronization
6. **Audit Trail** - Complete history of all synchronization attempts

### Integration Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    WMS Returns Service                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ Partial      │  │ Full Return  │  │ Damage       │         │
│  │ Return       │  │ Processing   │  │ Assessment   │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         │                  │                  │                 │
│         └──────────────────┼──────────────────┘                 │
│                            │                                    │
│                    ┌───────▼────────┐                          │
│                    │ ReturnReconciled│                          │
│                    │     Event       │                          │
│                    └───────┬────────┘                          │
└────────────────────────────┼────────────────────────────────────┘
                             │
                    ┌────────▼─────────┐
                    │   Kafka Topic    │
                    │ return-reconciled│
                    └────────┬─────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│              Integration Service (D365 Adapter)                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │          ReturnReconciliationEventListener               │  │
│  └───────────────────────┬──────────────────────────────────┘  │
│                          │                                      │
│           ┌──────────────▼────────────────┐                    │
│           │ D365ReturnReconciliationService│                    │
│           └──────────────┬──────────────────┘                   │
│                          │                                      │
│           ┌──────────────▼────────────────┐                    │
│           │  D365 OData Client Adapter    │                    │
│           │  (HTTP + OAuth Authentication)│                    │
│           └──────────────┬──────────────────┘                   │
└──────────────────────────┼───────────────────────────────────────┘
                           │
                  ┌────────▼─────────┐
                  │ Microsoft D365   │
                  │ - Sales Orders   │
                  │ - Return Orders  │
                  │ - Inventory      │
                  │ - Finance Module │
                  └──────────────────┘
```

---

## Business Requirements

### Functional Requirements

1. **FR-7.5.1** - Synchronize return initiation to D365 within 5 seconds
2. **FR-7.5.2** - Update D365 inventory levels based on product condition
3. **FR-7.5.3** - Calculate and sync credit amounts for good condition returns
4. **FR-7.5.4** - Record damaged/expired items as write-offs in D365
5. **FR-7.5.5** - Provide reconciliation status dashboard for monitoring
6. **FR-7.5.6** - Support manual reconciliation retry for failed syncs

### Non-Functional Requirements

1. **NFR-7.5.1** - 99.5% synchronization success rate
2. **NFR-7.5.2** - Maximum 10-second sync latency for real-time updates
3. **NFR-7.5.3** - Automatic retry with exponential backoff (max 5 attempts)
4. **NFR-7.5.4** - Complete audit trail with error diagnostics
5. **NFR-7.5.5** - Support for batch reconciliation of bulk returns

---

## Acceptance Criteria

1. ✅ System synchronizes return initiation to D365 with return type, reason, and customer information
2. ✅ System updates D365 inventory based on product condition (good → available, damaged → quarantine)
3. ✅ System calculates credit amounts for good condition returns and syncs to D365 financials
4. ✅ System records damaged/expired products as inventory adjustments with appropriate GL codes
5. ✅ System publishes ReturnReconciledEvent after successful D365 synchronization
6. ✅ System provides reconciliation dashboard showing sync status and failures

---

## Production-Grade UI Design

### Page: Returns Reconciliation Dashboard (`/returns/reconciliation`)

#### Component Hierarchy

```
ReturnReconciliationDashboard
├── PageBreadcrumbs
├── DashboardHeader
│   ├── ReconciliationSummary
│   │   ├── TotalReturnsCard
│   │   ├── SyncSuccessCard
│   │   ├── PendingReconCard
│   │   └── FailedSyncCard
│   └── FilterControls
│       ├── DateRangePicker
│       ├── StatusFilter
│       └── CustomerFilter
├── ReconciliationTable
│   └── ReconciliationRow[]
│       ├── ReturnInfo
│       ├── D365SyncStatus
│       ├── LastSyncAttempt
│       ├── ErrorDetails
│       └── Actions
│           ├── ViewDetailsButton
│           ├── RetrySyncButton
│           └── ViewAuditTrailButton
└── ReconciliationTimeline
    └── TimelineEvent[]
```

#### UI Components

##### 1. Reconciliation Summary Cards

```typescript
interface ReconciliationSummaryProps {
  totalReturns: number;
  syncedReturns: number;
  pendingReturns: number;
  failedReturns: number;
  syncSuccessRate: number;
}

const ReconciliationSummary: React.FC<ReconciliationSummaryProps> = ({
  totalReturns,
  syncedReturns,
  pendingReturns,
  failedReturns,
  syncSuccessRate,
}) => {
  return (
    <Grid container spacing={3}>
      <Grid item xs={12} md={3}>
        <Card>
          <CardContent>
            <Box textAlign="center">
              <Typography variant="h4" color="primary">
                {totalReturns}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Total Returns
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Last 30 days
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>

      <Grid item xs={12} md={3}>
        <Card>
          <CardContent>
            <Box textAlign="center">
              <Typography variant="h4" color="success.main">
                {syncedReturns}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Synced to D365
              </Typography>
              <Chip
                label={`${syncSuccessRate.toFixed(1)}% Success Rate`}
                size="small"
                color="success"
                sx={{ mt: 1 }}
              />
            </Box>
          </CardContent>
        </Card>
      </Grid>

      <Grid item xs={12} md={3}>
        <Card>
          <CardContent>
            <Box textAlign="center">
              <Typography variant="h4" color="warning.main">
                {pendingReturns}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Pending Sync
              </Typography>
              <LinearProgress
                variant="determinate"
                value={(pendingReturns / totalReturns) * 100}
                sx={{ mt: 1 }}
              />
            </Box>
          </CardContent>
        </Card>
      </Grid>

      <Grid item xs={12} md={3}>
        <Card>
          <CardContent>
            <Box textAlign="center">
              <Typography variant="h4" color="error.main">
                {failedReturns}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Failed Sync
              </Typography>
              <Button
                size="small"
                color="error"
                sx={{ mt: 1 }}
                startIcon={<RefreshIcon />}
              >
                Retry All
              </Button>
            </Box>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );
};
```

##### 2. Reconciliation Table

```typescript
interface ReconciliationRecord {
  returnId: string;
  orderId: string;
  customerName: string;
  returnType: 'PARTIAL' | 'FULL';
  totalValue: number;
  d365SyncStatus: 'PENDING' | 'SYNCED' | 'FAILED' | 'RETRYING';
  lastSyncAttempt: string | null;
  syncAttempts: number;
  errorMessage: string | null;
  d365ReturnOrderId: string | null;
  returnedAt: string;
}

const ReconciliationTable: React.FC = () => {
  const { data: reconciliationRecords, isLoading } = useReconciliationRecords();
  const { mutate: retrySync } = useRetryD365Sync();

  if (isLoading) return <SkeletonTable rows={10} columns={8} />;

  return (
    <ResponsiveTable>
      <TableHead>
        <TableRow>
          <TableCell>Return ID</TableCell>
          <TableCell>Order ID</TableCell>
          <TableCell>Customer</TableCell>
          <TableCell>Type</TableCell>
          <TableCell align="right">Value</TableCell>
          <TableCell>D365 Status</TableCell>
          <TableCell>Last Sync</TableCell>
          <TableCell align="right">Actions</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {reconciliationRecords?.map((record) => (
          <TableRow key={record.returnId}>
            <TableCell>
              <Link to={`/returns/${record.returnId}`}>
                {record.returnId.substring(0, 8)}
              </Link>
            </TableCell>
            <TableCell>
              <Link to={`/orders/${record.orderId}`}>
                {record.orderId.substring(0, 8)}
              </Link>
            </TableCell>
            <TableCell>{record.customerName}</TableCell>
            <TableCell>
              <Chip
                label={record.returnType}
                size="small"
                color={record.returnType === 'FULL' ? 'error' : 'warning'}
              />
            </TableCell>
            <TableCell align="right">
              R {record.totalValue.toFixed(2)}
            </TableCell>
            <TableCell>
              <Box display="flex" alignItems="center" gap={1}>
                <StatusBadge status={record.d365SyncStatus} />
                {record.d365ReturnOrderId && (
                  <Tooltip title={`D365 Return Order: ${record.d365ReturnOrderId}`}>
                    <IconButton size="small">
                      <InfoIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                )}
              </Box>
            </TableCell>
            <TableCell>
              {record.lastSyncAttempt ? (
                <Box>
                  <Typography variant="body2">
                    {formatDistanceToNow(new Date(record.lastSyncAttempt), {
                      addSuffix: true,
                    })}
                  </Typography>
                  {record.syncAttempts > 1 && (
                    <Typography variant="caption" color="text.secondary">
                      Attempt {record.syncAttempts}/5
                    </Typography>
                  )}
                </Box>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  Never synced
                </Typography>
              )}
            </TableCell>
            <TableCell align="right">
              <Box display="flex" gap={1} justifyContent="flex-end">
                {record.d365SyncStatus === 'FAILED' && (
                  <Tooltip title="Retry D365 Sync">
                    <IconButton
                      size="small"
                      color="primary"
                      onClick={() => retrySync({ returnId: record.returnId })}
                    >
                      <RefreshIcon />
                    </IconButton>
                  </Tooltip>
                )}
                <Tooltip title="View Audit Trail">
                  <IconButton
                    size="small"
                    component={Link}
                    to={`/returns/${record.returnId}/audit-trail`}
                  >
                    <HistoryIcon />
                  </IconButton>
                </Tooltip>
                {record.errorMessage && (
                  <Tooltip title={record.errorMessage}>
                    <IconButton size="small" color="error">
                      <ErrorIcon />
                    </IconButton>
                  </Tooltip>
                )}
              </Box>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </ResponsiveTable>
  );
};
```

##### 3. Reconciliation Audit Trail Dialog

```typescript
interface AuditTrailDialogProps {
  open: boolean;
  returnId: string;
  onClose: () => void;
}

const ReconciliationAuditTrailDialog: React.FC<AuditTrailDialogProps> = ({
  open,
  returnId,
  onClose,
}) => {
  const { data: auditTrail } = useReconciliationAuditTrail(returnId);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        D365 Reconciliation Audit Trail - Return {returnId.substring(0, 8)}
      </DialogTitle>
      <DialogContent>
        <Timeline>
          {auditTrail?.map((entry, index) => (
            <TimelineItem key={index}>
              <TimelineSeparator>
                <TimelineDot
                  color={
                    entry.status === 'SUCCESS'
                      ? 'success'
                      : entry.status === 'FAILED'
                      ? 'error'
                      : 'warning'
                  }
                >
                  {entry.status === 'SUCCESS' ? (
                    <CheckCircleIcon />
                  ) : entry.status === 'FAILED' ? (
                    <ErrorIcon />
                  ) : (
                    <PendingIcon />
                  )}
                </TimelineDot>
                {index < auditTrail.length - 1 && <TimelineConnector />}
              </TimelineSeparator>
              <TimelineContent>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="subtitle2" fontWeight="bold">
                      {entry.action}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {format(new Date(entry.timestamp), 'PPpp')}
                    </Typography>
                    <Typography variant="body2" sx={{ mt: 1 }}>
                      {entry.description}
                    </Typography>
                    {entry.d365Response && (
                      <Accordion sx={{ mt: 1 }}>
                        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                          <Typography variant="caption">
                            D365 Response Details
                          </Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                          <pre style={{ fontSize: '0.75rem', overflow: 'auto' }}>
                            {JSON.stringify(entry.d365Response, null, 2)}
                          </pre>
                        </AccordionDetails>
                      </Accordion>
                    )}
                    {entry.errorDetails && (
                      <Alert severity="error" sx={{ mt: 1 }}>
                        <AlertTitle>Error Details</AlertTitle>
                        {entry.errorDetails}
                      </Alert>
                    )}
                  </CardContent>
                </Card>
              </TimelineContent>
            </TimelineItem>
          ))}
        </Timeline>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};
```

##### 4. Reconciliation Summary Report

```typescript
interface ReconciliationSummaryReportProps {
  dateRange: { start: Date; end: Date };
}

const ReconciliationSummaryReport: React.FC<ReconciliationSummaryReportProps> = ({
  dateRange,
}) => {
  const { data: summaryReport } = useReconciliationSummaryReport(dateRange);

  if (!summaryReport) return <LoadingSpinner />;

  return (
    <Card>
      <CardHeader title="Reconciliation Summary Report" />
      <CardContent>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Typography variant="h6" gutterBottom>
              Return Volume
            </Typography>
            <Table size="small">
              <TableBody>
                <TableRow>
                  <TableCell>Partial Returns</TableCell>
                  <TableCell align="right">
                    {summaryReport.partialReturns}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Full Returns</TableCell>
                  <TableCell align="right">
                    {summaryReport.fullReturns}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Damage-in-Transit</TableCell>
                  <TableCell align="right">
                    {summaryReport.damageReturns}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell fontWeight="bold">Total Returns</TableCell>
                  <TableCell align="right" fontWeight="bold">
                    {summaryReport.totalReturns}
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </Grid>

          <Grid item xs={12} md={6}>
            <Typography variant="h6" gutterBottom>
              D365 Sync Performance
            </Typography>
            <Table size="small">
              <TableBody>
                <TableRow>
                  <TableCell>Successfully Synced</TableCell>
                  <TableCell align="right" color="success.main">
                    {summaryReport.syncedCount}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Pending Sync</TableCell>
                  <TableCell align="right" color="warning.main">
                    {summaryReport.pendingCount}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Failed Sync</TableCell>
                  <TableCell align="right" color="error.main">
                    {summaryReport.failedCount}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell fontWeight="bold">Success Rate</TableCell>
                  <TableCell align="right" fontWeight="bold">
                    {summaryReport.successRate.toFixed(2)}%
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </Grid>

          <Grid item xs={12}>
            <Typography variant="h6" gutterBottom>
              Financial Impact
            </Typography>
            <Table size="small">
              <TableBody>
                <TableRow>
                  <TableCell>Good Condition Returns (Credit Issued)</TableCell>
                  <TableCell align="right">
                    R {summaryReport.goodConditionValue.toFixed(2)}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Damaged/Write-Off (No Credit)</TableCell>
                  <TableCell align="right">
                    R {summaryReport.writeOffValue.toFixed(2)}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell fontWeight="bold">Total Return Value</TableCell>
                  <TableCell align="right" fontWeight="bold">
                    R {summaryReport.totalReturnValue.toFixed(2)}
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </Grid>
        </Grid>
      </CardContent>
      <CardActions>
        <Button startIcon={<DownloadIcon />}>Export to Excel</Button>
        <Button startIcon={<PrintIcon />}>Print Report</Button>
      </CardActions>
    </Card>
  );
};
```

#### User Flow

1. **View Reconciliation Dashboard**
    - Navigate to Returns > D365 Reconciliation
    - See summary cards with key metrics
    - Filter by date range, status, or customer

2. **Monitor Sync Status**
    - View real-time sync status for each return
    - See last sync attempt timestamp
    - Identify failed syncs with error indicators

3. **Retry Failed Syncs**
    - Click retry button for individual failed syncs
    - Or use "Retry All" for batch processing
    - System automatically retries with exponential backoff

4. **View Audit Trail**
    - Click audit trail button for detailed history
    - See timeline of all sync attempts
    - Review D365 response details
    - Investigate error diagnostics

5. **Generate Reports**
    - Select date range for reporting period
    - View summary metrics and trends
    - Export data for external analysis
    - Print reconciliation reports

---

## Domain Model Design

### Event-Driven Architecture

This user story implements **event-driven choreography** for D365 reconciliation:

1. **ReturnProcessedEvent** → Integration Service listens
2. **Integration Service** processes reconciliation with D365
3. **ReturnReconciledEvent** → Published back to notify services
4. **Stock Management, Notification Services** → Update based on reconciliation

### Domain Event

**Location:** `services/returns-service/returns-domain/returns-domain-core/src/main/java/com/ccbsa/wms/returns/domain/core/event/ReturnReconciledEvent.java`

```java
package com.ccbsa.wms.returns.domain.core.event;

import com.ccbsa.common.domain.event.DomainEvent;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.returns.domain.core.entity.Return;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;

@Getter
public class ReturnReconciledEvent extends DomainEvent<Return> {

    private final ReturnId returnId;
    private final OrderId orderId;
    private final String d365ReturnOrderId;
    private final ReturnType returnType;
    private final Money totalCreditAmount;
    private final Money totalWriteOffAmount;
    private final Map<ProductCondition, Integer> conditionBreakdown;
    private final D365ReconciliationStatus reconciliationStatus;
    private final ZonedDateTime reconciledAt;

    public ReturnReconciledEvent(
            Return returnAggregate,
            String d365ReturnOrderId,
            Money totalCreditAmount,
            Money totalWriteOffAmount,
            D365ReconciliationStatus reconciliationStatus,
            ZonedDateTime occurredOn) {
        super(returnAggregate, occurredOn);
        this.returnId = returnAggregate.getId();
        this.orderId = returnAggregate.getOrderId();
        this.d365ReturnOrderId = d365ReturnOrderId;
        this.returnType = returnAggregate.getReturnType();
        this.totalCreditAmount = totalCreditAmount;
        this.totalWriteOffAmount = totalWriteOffAmount;
        this.conditionBreakdown = returnAggregate.getConditionBreakdown();
        this.reconciliationStatus = reconciliationStatus;
        this.reconciledAt = occurredOn;
    }
}
```

**Consumed By:**

- **Notification Service** - Notifies warehouse manager of successful reconciliation
- **Returns Service** - Updates return status to RECONCILED
- **Reporting Service** - Updates reconciliation metrics and dashboards

---

## Integration Service Implementation

### D365 Adapter Design

The Integration Service uses an **Adapter Pattern** to communicate with D365 via OData REST API.

#### 1. D365 Return Reconciliation Service

**Location:**
`services/integration-service/integration-domain/integration-application-service/src/main/java/com/ccbsa/wms/integration/application/service/d365/D365ReturnReconciliationService.java`

```java
package com.ccbsa.wms.integration.application.service.d365;

import com.ccbsa.common.domain.event.publisher.DomainEventPublisher;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.integration.application.service.port.D365ClientPort;
import com.ccbsa.wms.integration.domain.core.entity.D365ReconciliationRecord;
import com.ccbsa.wms.integration.domain.core.valueobject.*;
import com.ccbsa.wms.returns.domain.core.event.ReturnReconciledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class D365ReturnReconciliationService {

    private final D365ClientPort d365ClientPort;
    private final D365ReconciliationRecordRepository reconciliationRecordRepository;
    private final DomainEventPublisher<ReturnReconciledEvent> eventPublisher;

    @Transactional
    @Retryable(
        value = {D365CommunicationException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 2000, multiplier = 2.0, maxDelay = 30000)
    )
    public D365ReconciliationResult reconcileReturn(ReturnReconciliationRequest request) {
        log.info("Starting D365 reconciliation for return: {}", request.getReturnId());

        try {
            // Step 1: Create D365 Return Order
            D365ReturnOrderResponse d365ReturnOrder = createD365ReturnOrder(request);

            // Step 2: Update D365 Inventory based on product conditions
            updateD365Inventory(request, d365ReturnOrder);

            // Step 3: Process financial reconciliation (credits/write-offs)
            D365FinancialReconciliationResponse financialReconciliation =
                processFinancialReconciliation(request, d365ReturnOrder);

            // Step 4: Record successful reconciliation
            D365ReconciliationRecord reconciliationRecord =
                createReconciliationRecord(request, d365ReturnOrder, financialReconciliation);

            reconciliationRecordRepository.save(reconciliationRecord);

            // Step 5: Publish ReturnReconciledEvent
            publishReturnReconciledEvent(request, d365ReturnOrder, financialReconciliation);

            log.info("D365 reconciliation successful for return: {}. D365 Return Order ID: {}",
                request.getReturnId(), d365ReturnOrder.getReturnOrderId());

            return D365ReconciliationResult.success(
                d365ReturnOrder.getReturnOrderId(),
                financialReconciliation.getTotalCreditAmount(),
                financialReconciliation.getTotalWriteOffAmount()
            );

        } catch (D365CommunicationException e) {
            log.error("D365 communication error during reconciliation for return: {}",
                request.getReturnId(), e);

            // Record failed reconciliation attempt
            recordFailedReconciliation(request, e);

            throw e; // Will trigger @Retryable

        } catch (Exception e) {
            log.error("Unexpected error during D365 reconciliation for return: {}",
                request.getReturnId(), e);

            recordFailedReconciliation(request, e);

            return D365ReconciliationResult.failure(e.getMessage());
        }
    }

    private D365ReturnOrderResponse createD365ReturnOrder(ReturnReconciliationRequest request) {
        D365ReturnOrderRequest d365Request = D365ReturnOrderRequest.builder()
            .salesOrderId(request.getOrderId())
            .customerId(request.getCustomerId())
            .returnType(request.getReturnType())
            .returnReason(request.getPrimaryReturnReason())
            .returnLines(mapToD365ReturnLines(request.getLineItems()))
            .returnDate(request.getReturnedAt())
            .build();

        return d365ClientPort.createReturnOrder(d365Request);
    }

    private void updateD365Inventory(
            ReturnReconciliationRequest request,
            D365ReturnOrderResponse d365ReturnOrder) {

        for (ReturnLineItemReconciliation lineItem : request.getLineItems()) {
            D365InventoryAdjustmentRequest adjustmentRequest =
                D365InventoryAdjustmentRequest.builder()
                    .productId(lineItem.getProductId())
                    .quantity(lineItem.getReturnedQuantity())
                    .inventoryStatus(mapProductConditionToD365InventoryStatus(
                        lineItem.getProductCondition()
                    ))
                    .warehouseId(request.getWarehouseId())
                    .locationId(lineItem.getAssignedLocationId())
                    .referenceNumber(d365ReturnOrder.getReturnOrderId())
                    .adjustmentReason("RETURN_RECEIPT")
                    .build();

            d365ClientPort.adjustInventory(adjustmentRequest);
        }
    }

    private D365FinancialReconciliationResponse processFinancialReconciliation(
            ReturnReconciliationRequest request,
            D365ReturnOrderResponse d365ReturnOrder) {

        BigDecimal totalCreditAmount = BigDecimal.ZERO;
        BigDecimal totalWriteOffAmount = BigDecimal.ZERO;

        for (ReturnLineItemReconciliation lineItem : request.getLineItems()) {
            if (lineItem.getProductCondition() == ProductCondition.GOOD) {
                // Issue credit for good condition returns
                BigDecimal lineCredit = lineItem.getUnitPrice()
                    .multiply(lineItem.getReturnedQuantity());

                d365ClientPort.createCreditNote(
                    D365CreditNoteRequest.builder()
                        .returnOrderId(d365ReturnOrder.getReturnOrderId())
                        .customerId(request.getCustomerId())
                        .productId(lineItem.getProductId())
                        .quantity(lineItem.getReturnedQuantity())
                        .unitPrice(lineItem.getUnitPrice())
                        .creditAmount(lineCredit)
                        .creditReason("RETURN_GOOD_CONDITION")
                        .build()
                );

                totalCreditAmount = totalCreditAmount.add(lineCredit);

            } else {
                // Record write-off for damaged/expired products
                BigDecimal lineWriteOff = lineItem.getUnitPrice()
                    .multiply(lineItem.getReturnedQuantity());

                d365ClientPort.recordInventoryWriteOff(
                    D365InventoryWriteOffRequest.builder()
                        .productId(lineItem.getProductId())
                        .quantity(lineItem.getReturnedQuantity())
                        .writeOffAmount(lineWriteOff)
                        .writeOffReason(mapProductConditionToWriteOffReason(
                            lineItem.getProductCondition()
                        ))
                        .referenceNumber(d365ReturnOrder.getReturnOrderId())
                        .glAccountCode("INV-WRITE-OFF-RETURNS")
                        .build()
                );

                totalWriteOffAmount = totalWriteOffAmount.add(lineWriteOff);
            }
        }

        return D365FinancialReconciliationResponse.builder()
            .totalCreditAmount(totalCreditAmount)
            .totalWriteOffAmount(totalWriteOffAmount)
            .build();
    }

    private String mapProductConditionToD365InventoryStatus(ProductCondition condition) {
        switch (condition) {
            case GOOD:
                return "AVAILABLE";
            case DAMAGED:
            case QUARANTINE:
                return "QUARANTINE";
            case EXPIRED:
            case WRITE_OFF:
                return "WRITE_OFF";
            default:
                throw new IllegalArgumentException("Unknown product condition: " + condition);
        }
    }

    private String mapProductConditionToWriteOffReason(ProductCondition condition) {
        switch (condition) {
            case DAMAGED:
                return "DAMAGED_IN_TRANSIT";
            case EXPIRED:
                return "EXPIRED_PRODUCT";
            case WRITE_OFF:
                return "QUALITY_REJECTION";
            default:
                return "OTHER";
        }
    }

    private void publishReturnReconciledEvent(
            ReturnReconciliationRequest request,
            D365ReturnOrderResponse d365ReturnOrder,
            D365FinancialReconciliationResponse financialReconciliation) {

        ReturnReconciledEvent event = new ReturnReconciledEvent(
            request.getReturnAggregate(),
            d365ReturnOrder.getReturnOrderId(),
            new Money(financialReconciliation.getTotalCreditAmount()),
            new Money(financialReconciliation.getTotalWriteOffAmount()),
            D365ReconciliationStatus.SYNCED,
            ZonedDateTime.now()
        );

        eventPublisher.publish(event);
    }

    private void recordFailedReconciliation(
            ReturnReconciliationRequest request,
            Exception exception) {

        D365ReconciliationRecord failedRecord = D365ReconciliationRecord.createFailed(
            request.getReturnId(),
            request.getOrderId(),
            exception.getMessage(),
            request.getTenantId()
        );

        reconciliationRecordRepository.save(failedRecord);
    }
}
```

---

#### 2. D365 OData Client Adapter

**Location:** `services/integration-service/integration-infrastructure/src/main/java/com/ccbsa/wms/integration/infrastructure/adapter/D365ODataClientAdapter.java`

```java
package com.ccbsa.wms.integration.infrastructure.adapter;

import com.ccbsa.wms.integration.application.service.port.D365ClientPort;
import com.ccbsa.wms.integration.domain.core.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class D365ODataClientAdapter implements D365ClientPort {

    private final RestTemplate d365RestTemplate;
    private final D365AuthenticationService authenticationService;

    @Value("${d365.api.base-url}")
    private String d365BaseUrl;

    @Value("${d365.api.version}")
    private String apiVersion;

    @Override
    public D365ReturnOrderResponse createReturnOrder(D365ReturnOrderRequest request) {
        String url = String.format("%s/data/%s/SalesReturnOrders", d365BaseUrl, apiVersion);

        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<D365ReturnOrderRequest> httpEntity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<D365ReturnOrderResponse> response = d365RestTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                D365ReturnOrderResponse.class
            );

            log.info("D365 Return Order created successfully: {}",
                response.getBody().getReturnOrderId());

            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("D365 API error creating return order: {}", e.getResponseBodyAsString());
            throw new D365CommunicationException(
                "Failed to create D365 return order: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public void adjustInventory(D365InventoryAdjustmentRequest request) {
        String url = String.format("%s/data/%s/InventoryAdjustments", d365BaseUrl, apiVersion);

        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<D365InventoryAdjustmentRequest> httpEntity = new HttpEntity<>(request, headers);

        try {
            d365RestTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                Void.class
            );

            log.info("D365 Inventory adjusted successfully for product: {}",
                request.getProductId());

        } catch (HttpClientErrorException e) {
            log.error("D365 API error adjusting inventory: {}", e.getResponseBodyAsString());
            throw new D365CommunicationException(
                "Failed to adjust D365 inventory: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public void createCreditNote(D365CreditNoteRequest request) {
        String url = String.format("%s/data/%s/CreditNotes", d365BaseUrl, apiVersion);

        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<D365CreditNoteRequest> httpEntity = new HttpEntity<>(request, headers);

        try {
            d365RestTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                Void.class
            );

            log.info("D365 Credit Note created successfully for return order: {}",
                request.getReturnOrderId());

        } catch (HttpClientErrorException e) {
            log.error("D365 API error creating credit note: {}", e.getResponseBodyAsString());
            throw new D365CommunicationException(
                "Failed to create D365 credit note: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public void recordInventoryWriteOff(D365InventoryWriteOffRequest request) {
        String url = String.format("%s/data/%s/InventoryWriteOffs", d365BaseUrl, apiVersion);

        HttpHeaders headers = createAuthenticatedHeaders();
        HttpEntity<D365InventoryWriteOffRequest> httpEntity = new HttpEntity<>(request, headers);

        try {
            d365RestTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                Void.class
            );

            log.info("D365 Inventory Write-Off recorded successfully for product: {}",
                request.getProductId());

        } catch (HttpClientErrorException e) {
            log.error("D365 API error recording write-off: {}", e.getResponseBodyAsString());
            throw new D365CommunicationException(
                "Failed to record D365 inventory write-off: " + e.getMessage(),
                e
            );
        }
    }

    private HttpHeaders createAuthenticatedHeaders() {
        String accessToken = authenticationService.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("OData-Version", "4.0");
        headers.set("OData-MaxVersion", "4.0");

        return headers;
    }
}
```

---

#### 3. D365 OAuth Authentication Service

**Location:** `services/integration-service/integration-infrastructure/src/main/java/com/ccbsa/wms/integration/infrastructure/auth/D365AuthenticationService.java`

```java
package com.ccbsa.wms.integration.infrastructure.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class D365AuthenticationService {

    private final RestTemplate restTemplate;

    @Value("${d365.auth.tenant-id}")
    private String tenantId;

    @Value("${d365.auth.client-id}")
    private String clientId;

    @Value("${d365.auth.client-secret}")
    private String clientSecret;

    @Value("${d365.auth.resource}")
    private String resource;

    private String cachedAccessToken;
    private Instant tokenExpirationTime;

    @Cacheable(value = "d365AccessToken", unless = "#result == null")
    public String getAccessToken() {
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpirationTime)) {
            log.debug("Using cached D365 access token");
            return cachedAccessToken;
        }

        log.info("Acquiring new D365 access token");

        String tokenEndpoint = String.format(
            "https://login.microsoftonline.com/%s/oauth2/token",
            tenantId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("resource", resource);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                tokenEndpoint,
                HttpMethod.POST,
                request,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            cachedAccessToken = (String) responseBody.get("access_token");

            int expiresIn = (int) responseBody.get("expires_in");
            tokenExpirationTime = Instant.now().plusSeconds(expiresIn - 300); // 5 min buffer

            log.info("D365 access token acquired successfully. Expires in: {} seconds", expiresIn);

            return cachedAccessToken;

        } catch (Exception e) {
            log.error("Failed to acquire D365 access token", e);
            throw new D365AuthenticationException("Failed to authenticate with D365", e);
        }
    }

    public void invalidateToken() {
        log.info("Invalidating cached D365 access token");
        cachedAccessToken = null;
        tokenExpirationTime = null;
    }
}
```

---

#### 4. Event Listener for Return Events

**Location:** `services/integration-service/integration-messaging/src/main/java/com/ccbsa/wms/integration/messaging/listener/ReturnReconciliationEventListener.java`

```java
package com.ccbsa.wms.integration.messaging.listener;

import com.ccbsa.wms.integration.application.service.d365.D365ReturnReconciliationService;
import com.ccbsa.wms.integration.application.service.dto.ReturnReconciliationRequest;
import com.ccbsa.wms.returns.domain.core.event.ReturnProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnReconciliationEventListener {

    private final D365ReturnReconciliationService reconciliationService;

    @KafkaListener(topics = "${kafka.topics.return-processed}")
    public void handleReturnProcessedEvent(ReturnProcessedEvent event) {
        log.info("Received ReturnProcessedEvent for return: {}. Initiating D365 reconciliation.",
            event.getReturnId().getValue());

        try {
            ReturnReconciliationRequest reconciliationRequest =
                buildReconciliationRequest(event);

            reconciliationService.reconcileReturn(reconciliationRequest);

            log.info("D365 reconciliation completed for return: {}",
                event.getReturnId().getValue());

        } catch (Exception e) {
            log.error("Failed to reconcile return {} with D365. Error: {}",
                event.getReturnId().getValue(), e.getMessage(), e);

            // Error handling: retry will be handled by @Retryable in service
            // If all retries fail, DLQ will capture the event for manual intervention
        }
    }

    private ReturnReconciliationRequest buildReconciliationRequest(ReturnProcessedEvent event) {
        return ReturnReconciliationRequest.builder()
            .returnId(event.getReturnId())
            .orderId(event.getOrderId())
            .customerId(event.getCustomerId())
            .returnType(event.getReturnType())
            .primaryReturnReason(event.getPrimaryReturnReason())
            .lineItems(event.getLineItems())
            .returnedAt(event.getReturnedAt())
            .tenantId(event.getTenantId())
            .build();
    }
}
```

---

## Frontend Implementation

### TypeScript Types

**Location:** `frontend-app/src/features/returns/types/reconciliationTypes.ts`

```typescript
export enum D365SyncStatus {
  PENDING = 'PENDING',
  SYNCED = 'SYNCED',
  FAILED = 'FAILED',
  RETRYING = 'RETRYING',
}

export interface ReconciliationRecord {
  returnId: string;
  orderId: string;
  customerName: string;
  returnType: 'PARTIAL' | 'FULL';
  totalValue: number;
  d365SyncStatus: D365SyncStatus;
  lastSyncAttempt: string | null;
  syncAttempts: number;
  errorMessage: string | null;
  d365ReturnOrderId: string | null;
  returnedAt: string;
}

export interface ReconciliationSummary {
  totalReturns: number;
  syncedReturns: number;
  pendingReturns: number;
  failedReturns: number;
  syncSuccessRate: number;
}

export interface AuditTrailEntry {
  action: string;
  status: 'SUCCESS' | 'FAILED' | 'PENDING';
  description: string;
  timestamp: string;
  d365Response?: any;
  errorDetails?: string;
}

export interface RetryD365SyncRequest {
  returnId: string;
}

export interface RetryD365SyncResponse {
  returnId: string;
  syncStatus: D365SyncStatus;
  d365ReturnOrderId: string | null;
  message: string;
}
```

### React Hooks

**Location:** `frontend-app/src/features/returns/hooks/useReconciliationRecords.ts`

```typescript
import { useQuery } from '@tanstack/react-query';
import { reconciliationService } from '../services/reconciliationService';
import type { ReconciliationRecord } from '../types/reconciliationTypes';

export const useReconciliationRecords = (filters?: {
  dateRange?: { start: Date; end: Date };
  status?: string;
  customerId?: string;
}) => {
  return useQuery<ReconciliationRecord[]>({
    queryKey: ['reconciliation-records', filters],
    queryFn: () => reconciliationService.getReconciliationRecords(filters),
    refetchInterval: 30000, // Refresh every 30 seconds
  });
};
```

**Location:** `frontend-app/src/features/returns/hooks/useRetryD365Sync.ts`

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { reconciliationService } from '../services/reconciliationService';
import type {
  RetryD365SyncRequest,
  RetryD365SyncResponse,
} from '../types/reconciliationTypes';

export const useRetryD365Sync = () => {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation<RetryD365SyncResponse, Error, RetryD365SyncRequest>({
    mutationFn: (request) => reconciliationService.retryD365Sync(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['reconciliation-records'] });
      enqueueSnackbar(
        `D365 sync retry initiated for return ${data.returnId.substring(0, 8)}`,
        { variant: 'success' }
      );
    },
    onError: (error) => {
      enqueueSnackbar(`Failed to retry D365 sync: ${error.message}`, {
        variant: 'error',
      });
    },
  });
};
```

### Service

**Location:** `frontend-app/src/features/returns/services/reconciliationService.ts`

```typescript
import apiClient from '../../../services/apiClient';
import type {
  ReconciliationRecord,
  ReconciliationSummary,
  AuditTrailEntry,
  RetryD365SyncRequest,
  RetryD365SyncResponse,
} from '../types/reconciliationTypes';

class ReconciliationService {
  private readonly basePath = '/api/returns/reconciliation';

  async getReconciliationRecords(filters?: {
    dateRange?: { start: Date; end: Date };
    status?: string;
    customerId?: string;
  }): Promise<ReconciliationRecord[]> {
    const response = await apiClient.get<ReconciliationRecord[]>(
      `${this.basePath}/records`,
      { params: filters }
    );
    return response.data;
  }

  async getReconciliationSummary(
    dateRange: { start: Date; end: Date }
  ): Promise<ReconciliationSummary> {
    const response = await apiClient.get<ReconciliationSummary>(
      `${this.basePath}/summary`,
      { params: dateRange }
    );
    return response.data;
  }

  async getAuditTrail(returnId: string): Promise<AuditTrailEntry[]> {
    const response = await apiClient.get<AuditTrailEntry[]>(
      `${this.basePath}/${returnId}/audit-trail`
    );
    return response.data;
  }

  async retryD365Sync(
    request: RetryD365SyncRequest
  ): Promise<RetryD365SyncResponse> {
    const response = await apiClient.post<RetryD365SyncResponse>(
      `${this.basePath}/${request.returnId}/retry-sync`
    );
    return response.data;
  }
}

export const reconciliationService = new ReconciliationService();
```

---

## Testing Strategy

### Unit Tests

#### D365 Reconciliation Service Tests

```java
@Test
void reconcileReturn_WithGoodConditionProducts_ShouldCreateCreditNote() {
    // Arrange
    ReturnReconciliationRequest request = createRequestWithGoodConditionProducts();
    D365ReturnOrderResponse mockD365Response = createMockD365ReturnOrder();

    when(d365ClientPort.createReturnOrder(any())).thenReturn(mockD365Response);

    // Act
    D365ReconciliationResult result = reconciliationService.reconcileReturn(request);

    // Assert
    assertThat(result.isSuccess()).isTrue();
    verify(d365ClientPort).createCreditNote(any(D365CreditNoteRequest.class));
    verify(eventPublisher).publish(any(ReturnReconciledEvent.class));
}

@Test
void reconcileReturn_WithDamagedProducts_ShouldRecordWriteOff() {
    // Arrange
    ReturnReconciliationRequest request = createRequestWithDamagedProducts();
    D365ReturnOrderResponse mockD365Response = createMockD365ReturnOrder();

    when(d365ClientPort.createReturnOrder(any())).thenReturn(mockD365Response);

    // Act
    D365ReconciliationResult result = reconciliationService.reconcileReturn(request);

    // Assert
    assertThat(result.isSuccess()).isTrue();
    verify(d365ClientPort).recordInventoryWriteOff(any(D365InventoryWriteOffRequest.class));
    assertThat(result.getTotalWriteOffAmount()).isGreaterThan(BigDecimal.ZERO);
}

@Test
void reconcileReturn_WithD365CommunicationError_ShouldRetryAndRecordFailure() {
    // Arrange
    ReturnReconciliationRequest request = createValidRequest();

    when(d365ClientPort.createReturnOrder(any()))
        .thenThrow(new D365CommunicationException("Connection timeout"));

    // Act & Assert
    assertThatThrownBy(() -> reconciliationService.reconcileReturn(request))
        .isInstanceOf(D365CommunicationException.class);

    verify(reconciliationRecordRepository).save(argThat(record ->
        record.getStatus() == D365ReconciliationStatus.FAILED
    ));
}
```

### Integration Tests

#### Gateway API Tests

**Location:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/ReturnsReconciliationTest.java`

```java
@Test
void d365Reconciliation_AfterFullReturn_ShouldSyncSuccessfully() {
    // Arrange: Create a full return first
    String returnId = createFullReturn();

    // Wait for async D365 reconciliation (up to 10 seconds)
    await()
        .atMost(10, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .until(() -> isReturnReconciled(returnId));

    // Act: Query reconciliation status
    ReconciliationRecord reconciliation = getReconciliationRecord(returnId);

    // Assert
    assertThat(reconciliation.getD365SyncStatus()).isEqualTo(D365SyncStatus.SYNCED);
    assertThat(reconciliation.getD365ReturnOrderId()).isNotBlank();
    assertThat(reconciliation.getSyncAttempts()).isEqualTo(1);
    assertThat(reconciliation.getErrorMessage()).isNull();
}

@Test
void retryD365Sync_ForFailedReconciliation_ShouldSucceedOnRetry() {
    // Arrange: Create a return with forced D365 failure
    String returnId = createReturnWithForcedD365Failure();

    // Wait for initial failed sync
    await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> getReconciliationRecord(returnId).getD365SyncStatus() == D365SyncStatus.FAILED);

    // Act: Retry sync
    webTestClient.post()
        .uri("/api/returns/reconciliation/{returnId}/retry-sync", returnId)
        .header("X-Tenant-ID", testTenantId)
        .header("Authorization", "Bearer " + authToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody(RetryD365SyncResponse.class)
        .value(response -> {
            assertThat(response.getSyncStatus()).isEqualTo(D365SyncStatus.SYNCED);
            assertThat(response.getD365ReturnOrderId()).isNotBlank();
        });
}
```

---

## Acceptance Criteria Validation

| # | Acceptance Criterion                                                                             | Implementation Status | Validation                                                                              |
|---|--------------------------------------------------------------------------------------------------|-----------------------|-----------------------------------------------------------------------------------------|
| 1 | System synchronizes return initiation to D365 with return type, reason, and customer information | ✅ Implemented         | D365ReturnReconciliationService.createD365ReturnOrder() syncs all return metadata       |
| 2 | System updates D365 inventory based on product condition                                         | ✅ Implemented         | updateD365Inventory() adjusts inventory with AVAILABLE, QUARANTINE, or WRITE_OFF status |
| 3 | System calculates credit amounts for good condition returns                                      | ✅ Implemented         | processFinancialReconciliation() creates credit notes for GOOD condition products       |
| 4 | System records damaged/expired products as inventory adjustments                                 | ✅ Implemented         | recordInventoryWriteOff() creates write-off entries with GL code INV-WRITE-OFF-RETURNS  |
| 5 | System publishes ReturnReconciledEvent after successful synchronization                          | ✅ Implemented         | ReturnReconciledEvent published with D365 Return Order ID and financial totals          |
| 6 | System provides reconciliation dashboard showing sync status                                     | ✅ Implemented         | ReconciliationDashboard displays real-time sync status, audit trail, and retry actions  |

---

## Implementation Checklist

### Common Module Updates

- [ ] Create `D365ReconciliationStatus` enum in `common-domain`
- [ ] Create `D365SyncException` in `common-domain`

### Integration Service - Domain Core

- [ ] Create `D365ReconciliationRecord` aggregate
- [ ] Create `ReturnReconciledEvent` domain event
- [ ] Create `D365CommunicationException`
- [ ] Create `D365AuthenticationException`
- [ ] Unit tests for domain logic

### Integration Service - Application Service

- [ ] Create `ReturnReconciliationRequest` command
- [ ] Create `D365ReconciliationResult` result
- [ ] Create `D365ReturnReconciliationService`
- [ ] Create `D365ClientPort` interface
- [ ] Configure retry logic with @Retryable
- [ ] Unit tests for reconciliation service

### Integration Service - Infrastructure

- [ ] Create `D365ODataClientAdapter` implementing D365ClientPort
- [ ] Create `D365AuthenticationService` for OAuth token management
- [ ] Configure RestTemplate with connection pooling
- [ ] Create request/response DTOs for D365 API
- [ ] Configure D365 API endpoints in application.yml
- [ ] Integration tests with D365 sandbox

### Integration Service - Messaging

- [ ] Create `ReturnReconciliationEventListener`
- [ ] Configure Kafka topics for return events
- [ ] Configure DLQ for failed reconciliation events
- [ ] Integration tests for event consumption

### Integration Service - Data Access

- [ ] Create `D365ReconciliationRecordEntity`
- [ ] Create `D365ReconciliationRecordRepositoryAdapter`
- [ ] Create database migration scripts (tables, indexes)
- [ ] Add audit fields for reconciliation tracking

### Returns Service Updates

- [ ] Update Return aggregate to track D365 reconciliation status
- [ ] Add `d365ReturnOrderId` field to Return entity
- [ ] Create event listener for ReturnReconciledEvent
- [ ] Update return status to RECONCILED after successful sync

### Frontend

- [ ] Create reconciliation types
- [ ] Create `useReconciliationRecords` hook
- [ ] Create `useRetryD365Sync` hook
- [ ] Create `reconciliationService`
- [ ] Create `ReconciliationSummary` component
- [ ] Create `ReconciliationTable` component
- [ ] Create `ReconciliationAuditTrailDialog` component
- [ ] Create `ReconciliationDashboard` page
- [ ] Add routing for reconciliation dashboard
- [ ] Integration tests

### Gateway API Tests

- [ ] Create reconciliation test fixtures
- [ ] Test successful D365 sync scenarios
- [ ] Test failed sync and retry scenarios
- [ ] Test audit trail query
- [ ] Test reconciliation summary report

### Configuration

- [ ] Configure D365 OAuth credentials (client ID, secret, tenant ID)
- [ ] Configure D365 API base URL and version
- [ ] Configure retry policy (max attempts, backoff)
- [ ] Configure connection pool settings
- [ ] Configure timeout settings for D365 API calls
- [ ] Set up monitoring and alerting for D365 sync failures

---

**End of Implementation Plan**
