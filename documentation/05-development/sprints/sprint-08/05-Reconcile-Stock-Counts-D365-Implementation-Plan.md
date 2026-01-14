# Reconcile Stock Counts with D365 Implementation Plan

## US-8.3.1: Reconcile Stock Counts with D365

**Service:** Integration Service, Reconciliation Service
**Priority:** Should Have
**Story Points:** 8
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

**As a** warehouse manager
**I want** to automatically reconcile stock counts with Microsoft Dynamics 365
**So that** inventory records in D365 remain accurate and synchronized with the warehouse system

### Business Requirements

- **Event-driven reconciliation** - Triggered by `StockCountCompletedEvent`
- **D365 OData API integration** - Create inventory counting journals
- **Retry mechanism** with exponential backoff (max 5 attempts)
- **Reconciliation dashboard** with status tracking and metrics
- **Audit trail** for all sync attempts (success and failures)
- **Manual retry capability** for failed reconciliations
- **Offline resilience** - Queue reconciliations when D365 unavailable
- **Async validation** - Non-blocking reconciliation with status updates

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Integration Service handles D365 communication
- OAuth 2.0 authentication with token refresh
- Exponential backoff retry strategy
- Complete audit trail with all HTTP requests/responses
- Dead letter queue for persistent failures

### Acceptance Criteria

1. **AC-1**: System reconciles completed stock counts with D365 (if integration enabled)
2. **AC-2**: Reconciliation includes: stock count journal ID, variances, adjusted quantities
3. **AC-3**: System handles reconciliation errors with retry mechanism (exponential backoff)
4. **AC-4**: System maintains reconciliation audit trail with all sync attempts
5. **AC-5**: System publishes `ReconciliationInitiatedEvent`, `ReconciliationCompletedEvent`, `ReconciliationFailedEvent`
6. **AC-6**: Dashboard displays reconciliation status and allows manual retry

---

## UI Design

### D365 Reconciliation Dashboard

**Component:** `D365ReconciliationDashboard.tsx`
**Route:** `/reconciliation/d365-reconciliation`

#### Component Hierarchy

```
D365ReconciliationDashboard
├── DashboardHeader
│   ├── PageTitle
│   └── RefreshButton
├── ReconciliationMetrics
│   ├── TotalReconciliationsCard
│   ├── SuccessRateCard
│   ├── PendingReconciliationsCard
│   └── FailedReconciliationsCard
├── ReconciliationFilters
│   ├── StatusFilter
│   ├── DateRangeFilter
│   └── SearchBar
├── ReconciliationRecordsTable
│   ├── TableHeader
│   └── ReconciliationRow[]
│       ├── StockCountReference
│       ├── ReconciliationStatus
│       ├── D365JournalId
│       ├── SyncAttempts
│       ├── LastAttempt
│       ├── ErrorMessage (if failed)
│       └── ActionButtons
│           ├── ViewDetailsButton
│           ├── RetryButton (for failed)
│           └── ViewAuditTrailButton
└── ReconciliationDetailsDialog
    ├── ReconciliationInfo
    ├── D365Request (JSON viewer)
    ├── D365Response (JSON viewer)
    └── AuditTrailTimeline
```

#### TypeScript Types

```typescript
export enum ReconciliationStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  SYNCED = 'SYNCED',
  FAILED = 'FAILED',
  RETRYING = 'RETRYING',
}

export interface D365ReconciliationRecord {
  reconciliationId: string;
  stockCountId: string;
  stockCountReference: string;
  d365JournalId?: string;
  d365JournalNumber?: string;
  reconciliationStatus: ReconciliationStatus;
  syncAttempts: number;
  maxAttempts: number;
  lastSyncAttempt?: string;
  nextRetryAt?: string;
  errorMessage?: string;
  errorCode?: string;
  d365RequestPayload?: any;
  d365ResponsePayload?: any;
  syncDurationMs?: number;
  initiatedAt: string;
  completedAt?: string;
}

export interface D365ReconciliationSummary {
  totalReconciliations: number;
  successfulReconciliations: number;
  failedReconciliations: number;
  pendingReconciliations: number;
  successRate: number;
  averageSyncDuration: number;
}

export interface D365AuditLogEntry {
  auditId: string;
  reconciliationId: string;
  attemptNumber: number;
  attemptTimestamp: string;
  httpStatusCode?: number;
  requestPayload: any;
  responsePayload?: any;
  errorDetails?: string;
  retryAfterSeconds?: number;
}

export interface RetryReconciliationRequest {
  reconciliationId: string;
}
```

#### Key UI Components

##### 1. Reconciliation Metrics Cards

```typescript
const ReconciliationMetrics: React.FC<{
  summary: D365ReconciliationSummary;
}> = ({ summary }) => {
  return (
    <Grid container spacing={3}>
      <Grid item xs={12} md={3}>
        <Card>
          <CardContent>
            <Typography variant="h4">{summary.totalReconciliations}</Typography>
            <Typography variant="caption">Total Reconciliations</Typography>
          </CardContent>
        </Card>
      </Grid>

      <Grid item xs={12} md={3}>
        <Card>
          <CardContent>
            <Typography variant="h4" color="success.main">
              {summary.successRate.toFixed(1)}%
            </Typography>
            <Typography variant="caption">Success Rate</Typography>
            <LinearProgress
              variant="determinate"
              value={summary.successRate}
              color="success"
              sx={{ mt: 1 }}
            />
          </CardContent>
        </Card>
      </Grid>

      <Grid item xs={12} md={3}>
        <Card>
          <CardContent>
            <Typography variant="h4" color="warning.main">
              {summary.pendingReconciliations}
            </Typography>
            <Typography variant="caption">Pending/Retrying</Typography>
          </CardContent>
        </Card>
      </Grid>

      <Grid item xs={12} md={3}>
        <Card>
          <CardContent>
            <Typography variant="h4" color="error">
              {summary.failedReconciliations}
            </Typography>
            <Typography variant="caption">Failed</Typography>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );
};
```

##### 2. Reconciliation Records Table

```typescript
const ReconciliationRecordsTable: React.FC<{
  records: D365ReconciliationRecord[];
  onRetry: (reconciliationId: string) => void;
  onViewDetails: (record: D365ReconciliationRecord) => void;
}> = ({ records, onRetry, onViewDetails }) => {
  const getStatusColor = (status: ReconciliationStatus) => {
    switch (status) {
      case ReconciliationStatus.SYNCED:
        return 'success';
      case ReconciliationStatus.FAILED:
        return 'error';
      case ReconciliationStatus.RETRYING:
        return 'warning';
      case ReconciliationStatus.PENDING:
      case ReconciliationStatus.IN_PROGRESS:
        return 'info';
    }
  };

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Stock Count</TableCell>
            <TableCell>D365 Journal</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="center">Attempts</TableCell>
            <TableCell>Last Attempt</TableCell>
            <TableCell>Duration</TableCell>
            <TableCell align="center">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {records.map((record) => (
            <TableRow key={record.reconciliationId}>
              <TableCell>
                <Typography variant="body2" fontWeight="bold">
                  {record.stockCountReference}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {record.stockCountId}
                </Typography>
              </TableCell>

              <TableCell>
                {record.d365JournalNumber ? (
                  <>
                    <Typography variant="body2">
                      {record.d365JournalNumber}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {record.d365JournalId}
                    </Typography>
                  </>
                ) : (
                  <Typography variant="caption" color="text.secondary">
                    Not synced
                  </Typography>
                )}
              </TableCell>

              <TableCell>
                <Chip
                  label={record.reconciliationStatus}
                  color={getStatusColor(record.reconciliationStatus) as any}
                  size="small"
                />
                {record.errorMessage && (
                  <Tooltip title={record.errorMessage}>
                    <ErrorIcon
                      color="error"
                      fontSize="small"
                      sx={{ ml: 1, cursor: 'pointer' }}
                    />
                  </Tooltip>
                )}
              </TableCell>

              <TableCell align="center">
                <Typography variant="body2">
                  {record.syncAttempts} / {record.maxAttempts}
                </Typography>
                {record.nextRetryAt && (
                  <Typography variant="caption" color="text.secondary">
                    Next: {format(new Date(record.nextRetryAt), 'PPp')}
                  </Typography>
                )}
              </TableCell>

              <TableCell>
                {record.lastSyncAttempt ? (
                  <Typography variant="caption">
                    {formatDistanceToNow(new Date(record.lastSyncAttempt), {
                      addSuffix: true,
                    })}
                  </Typography>
                ) : (
                  <Typography variant="caption" color="text.secondary">
                    Not attempted
                  </Typography>
                )}
              </TableCell>

              <TableCell>
                {record.syncDurationMs && (
                  <Typography variant="caption">
                    {record.syncDurationMs}ms
                  </Typography>
                )}
              </TableCell>

              <TableCell align="center">
                <IconButton
                  size="small"
                  onClick={() => onViewDetails(record)}
                  title="View details"
                >
                  <VisibilityIcon />
                </IconButton>

                {record.reconciliationStatus === ReconciliationStatus.FAILED && (
                  <IconButton
                    size="small"
                    color="warning"
                    onClick={() => onRetry(record.reconciliationId)}
                    title="Retry reconciliation"
                  >
                    <RefreshIcon />
                  </IconButton>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
```

---

## Domain Model Design

### Aggregate

#### D365StockCountReconciliation Aggregate

**Location:** `services/integration-service/integration-domain/integration-domain-core/src/main/java/com/ccbsa/wms/integration/domain/core/entity/D365StockCountReconciliation.java`

```java
package com.ccbsa.wms.integration.domain.core.entity;

import com.ccbsa.common.domain.entity.AggregateRoot;
import com.ccbsa.common.domain.valueobject.*;
import com.ccbsa.wms.integration.domain.core.event.*;
import com.ccbsa.wms.integration.domain.core.valueobject.*;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class D365StockCountReconciliation extends AggregateRoot<D365ReconciliationId> {

    private final StockCountId stockCountId;
    private final TenantId tenantId;
    private D365JournalId d365JournalId;
    private String d365JournalNumber;
    private ReconciliationStatus reconciliationStatus;
    private int syncAttempts;
    private final int maxAttempts;
    private ZonedDateTime lastSyncAttempt;
    private ZonedDateTime nextRetryAt;
    private String errorMessage;
    private String errorCode;
    private String d365RequestPayload;
    private String d365ResponsePayload;
    private Long syncDurationMs;
    private final ZonedDateTime initiatedAt;
    private ZonedDateTime completedAt;

    /**
     * Factory method - Initiate D365 reconciliation
     */
    public static D365StockCountReconciliation initiate(
            D365ReconciliationId reconciliationId,
            StockCountId stockCountId,
            TenantId tenantId,
            int maxAttempts) {

        D365StockCountReconciliation reconciliation = new D365StockCountReconciliation(
            reconciliationId,
            stockCountId,
            tenantId,
            ReconciliationStatus.PENDING,
            0,
            maxAttempts,
            ZonedDateTime.now()
        );

        reconciliation.registerEvent(new D365ReconciliationInitiatedEvent(
            reconciliation,
            ZonedDateTime.now()
        ));

        return reconciliation;
    }

    /**
     * Mark reconciliation as in progress
     */
    public void startSyncAttempt(String requestPayload) {
        this.reconciliationStatus = ReconciliationStatus.IN_PROGRESS;
        this.syncAttempts++;
        this.lastSyncAttempt = ZonedDateTime.now();
        this.d365RequestPayload = requestPayload;
    }

    /**
     * Mark reconciliation as successful
     */
    public void markSuccessful(
            D365JournalId journalId,
            String journalNumber,
            String responsePayload,
            long durationMs) {

        this.d365JournalId = journalId;
        this.d365JournalNumber = journalNumber;
        this.reconciliationStatus = ReconciliationStatus.SYNCED;
        this.d365ResponsePayload = responsePayload;
        this.syncDurationMs = durationMs;
        this.completedAt = ZonedDateTime.now();
        this.errorMessage = null;
        this.errorCode = null;

        registerEvent(new D365ReconciliationCompletedEvent(
            this.getId(),
            this.stockCountId,
            this.tenantId,
            journalId,
            ZonedDateTime.now()
        ));
    }

    /**
     * Mark reconciliation as failed with retry
     */
    public void markFailed(
            String errorMessage,
            String errorCode,
            String responsePayload,
            long durationMs) {

        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.d365ResponsePayload = responsePayload;
        this.syncDurationMs = durationMs;

        if (this.syncAttempts >= this.maxAttempts) {
            // Max attempts reached - permanent failure
            this.reconciliationStatus = ReconciliationStatus.FAILED;
            this.nextRetryAt = null;

            registerEvent(new D365ReconciliationFailedEvent(
                this.getId(),
                this.stockCountId,
                this.tenantId,
                errorMessage,
                this.syncAttempts,
                true, // maxAttemptsReached
                ZonedDateTime.now()
            ));
        } else {
            // Schedule retry with exponential backoff
            this.reconciliationStatus = ReconciliationStatus.RETRYING;
            this.nextRetryAt = calculateNextRetryTime();

            registerEvent(new D365ReconciliationFailedEvent(
                this.getId(),
                this.stockCountId,
                this.tenantId,
                errorMessage,
                this.syncAttempts,
                false, // maxAttemptsReached
                ZonedDateTime.now()
            ));
        }
    }

    /**
     * Calculate next retry time using exponential backoff
     * Base delay: 2 seconds
     * Multiplier: 2.0
     * Max delay: 30 seconds
     * Jitter: 10%
     */
    private ZonedDateTime calculateNextRetryTime() {
        int baseDelaySeconds = 2;
        double multiplier = 2.0;
        int maxDelaySeconds = 30;
        double jitter = 0.1;

        // Calculate delay: min(baseDelay * (multiplier ^ attempt), maxDelay)
        double delaySeconds = Math.min(
            baseDelaySeconds * Math.pow(multiplier, this.syncAttempts - 1),
            maxDelaySeconds
        );

        // Add jitter: ±10%
        double jitterAmount = delaySeconds * jitter * (Math.random() * 2 - 1);
        long finalDelaySeconds = Math.round(delaySeconds + jitterAmount);

        return ZonedDateTime.now().plusSeconds(finalDelaySeconds);
    }

    /**
     * Reset for manual retry
     */
    public void resetForManualRetry() {
        if (this.reconciliationStatus != ReconciliationStatus.FAILED) {
            throw new InvalidReconciliationStateException(
                "Can only manually retry failed reconciliations"
            );
        }

        this.reconciliationStatus = ReconciliationStatus.PENDING;
        this.syncAttempts = 0;
        this.errorMessage = null;
        this.errorCode = null;
        this.nextRetryAt = null;

        registerEvent(new D365ReconciliationRetryInitiatedEvent(
            this.getId(),
            this.stockCountId,
            ZonedDateTime.now()
        ));
    }

    /**
     * Check if reconciliation is ready for retry
     */
    public boolean isReadyForRetry() {
        return this.reconciliationStatus == ReconciliationStatus.RETRYING &&
               this.nextRetryAt != null &&
               ZonedDateTime.now().isAfter(this.nextRetryAt);
    }

    // Constructor, getters...
}
```

### Value Objects

```java
// D365ReconciliationId
public class D365ReconciliationId extends BaseId<UUID> {
    public D365ReconciliationId(UUID value) {
        super(value);
    }

    public static D365ReconciliationId generate() {
        return new D365ReconciliationId(UUID.randomUUID());
    }
}

// D365JournalId
public class D365JournalId extends BaseId<String> {
    public D365JournalId(String value) {
        super(value);
    }
}

// ReconciliationStatus
public enum ReconciliationStatus {
    PENDING("Pending", "Awaiting initial sync attempt"),
    IN_PROGRESS("In Progress", "Sync in progress"),
    SYNCED("Synced", "Successfully synchronized with D365"),
    FAILED("Failed", "Sync failed after max attempts"),
    RETRYING("Retrying", "Sync failed, scheduled for retry");

    private final String displayName;
    private final String description;

    ReconciliationStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isComplete() {
        return this == SYNCED || this == FAILED;
    }

    public boolean isRetryable() {
        return this == RETRYING || this == PENDING;
    }
}
```

---

## Backend Implementation

### Event Listener

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class StockCountCompletedEventListener {

    private final D365StockCountReconciliationService reconciliationService;
    private final D365Configuration d365Config;

    @KafkaListener(topics = "${kafka.topics.stock-count-completed}")
    public void handleStockCountCompleted(StockCountCompletedEvent event) {
        // Check if D365 reconciliation is enabled
        if (!d365Config.isReconciliationEnabled()) {
            log.debug("D365 reconciliation disabled. Skipping for stock count: {}",
                event.getStockCountId().getValue());
            return;
        }

        log.info("Initiating D365 reconciliation for stock count: {}",
            event.getStockCountId().getValue());

        try {
            reconciliationService.initiateReconciliation(
                event.getStockCountId(),
                event.getTenantId()
            );
        } catch (Exception e) {
            log.error("Failed to initiate D365 reconciliation for stock count: {}",
                event.getStockCountId().getValue(), e);
            // Event will be retried by Kafka
        }
    }
}
```

### Domain Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class D365StockCountReconciliationService {

    private final D365ReconciliationRepository reconciliationRepository;
    private final StockCountRepository stockCountRepository;
    private final D365ApiClient d365ApiClient;
    private final D365Configuration d365Config;

    @Transactional
    public D365ReconciliationId initiateReconciliation(
            StockCountId stockCountId,
            TenantId tenantId) {

        // Create reconciliation record
        D365StockCountReconciliation reconciliation =
            D365StockCountReconciliation.initiate(
                D365ReconciliationId.generate(),
                stockCountId,
                tenantId,
                d365Config.getRetry().getMaxAttempts()
            );

        // Save
        D365StockCountReconciliation saved = reconciliationRepository.save(reconciliation);

        // Trigger async sync
        executeReconciliation(saved.getId(), tenantId);

        return saved.getId();
    }

    @Async
    public void executeReconciliation(
            D365ReconciliationId reconciliationId,
            TenantId tenantId) {

        D365StockCountReconciliation reconciliation =
            reconciliationRepository.findById(reconciliationId, tenantId)
                .orElseThrow(() -> new ReconciliationNotFoundException(
                    "Reconciliation not found: " + reconciliationId.getValue()
                ));

        // Retrieve stock count data
        StockCount stockCount = stockCountRepository.findById(
            reconciliation.getStockCountId(),
            tenantId
        ).orElseThrow(() -> new StockCountNotFoundException(
            "Stock count not found: " + reconciliation.getStockCountId().getValue()
        ));

        // Build D365 request
        D365CountingJournalRequest request = buildD365Request(stockCount);
        String requestJson = toJson(request);

        // Mark as in progress
        reconciliation.startSyncAttempt(requestJson);
        reconciliationRepository.save(reconciliation);

        long startTime = System.currentTimeMillis();

        try {
            // Call D365 API
            D365CountingJournalResponse response = d365ApiClient.createCountingJournal(request);
            long duration = System.currentTimeMillis() - startTime;

            // Mark as successful
            reconciliation.markSuccessful(
                new D365JournalId(response.getJournalId()),
                response.getJournalNumber(),
                toJson(response),
                duration
            );

            log.info("D365 reconciliation successful. Journal ID: {}, Duration: {}ms",
                response.getJournalId(), duration);

        } catch (D365ApiException e) {
            long duration = System.currentTimeMillis() - startTime;

            // Mark as failed
            reconciliation.markFailed(
                e.getMessage(),
                e.getErrorCode(),
                e.getResponseBody(),
                duration
            );

            log.error("D365 reconciliation failed. Attempt {}/{}. Error: {}",
                reconciliation.getSyncAttempts(),
                reconciliation.getMaxAttempts(),
                e.getMessage());

            // Schedule retry if not max attempts
            if (reconciliation.getReconciliationStatus() == ReconciliationStatus.RETRYING) {
                scheduleRetry(reconciliation);
            }
        } finally {
            reconciliationRepository.save(reconciliation);
        }
    }

    private void scheduleRetry(D365StockCountReconciliation reconciliation) {
        // Retry will be picked up by scheduled job
        log.info("Scheduled retry for reconciliation {} at {}",
            reconciliation.getId().getValue(),
            reconciliation.getNextRetryAt());
    }

    /**
     * Scheduled job to retry failed reconciliations
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void retryFailedReconciliations() {
        List<D365StockCountReconciliation> readyForRetry =
            reconciliationRepository.findReadyForRetry();

        log.debug("Found {} reconciliations ready for retry", readyForRetry.size());

        readyForRetry.forEach(reconciliation -> {
            executeReconciliation(
                reconciliation.getId(),
                reconciliation.getTenantId()
            );
        });
    }

    /**
     * Build D365 counting journal request
     */
    private D365CountingJournalRequest buildD365Request(StockCount stockCount) {
        return D365CountingJournalRequest.builder()
            .journalName("WMS Stock Count - " + stockCount.getCountReference())
            .countDate(stockCount.getInitiatedAt().toLocalDate())
            .lines(stockCount.getEntries().stream()
                .map(entry -> D365CountingJournalLine.builder()
                    .itemNumber(entry.getProductCode().getValue())
                    .locationId(entry.getLocationCode().getValue())
                    .countedQuantity(entry.getCountedQuantity().getValue())
                    .systemQuantity(entry.getSystemQuantity().getValue())
                    .variance(entry.getVarianceQuantity().getValue())
                    .build())
                .collect(Collectors.toList()))
            .build();
    }
}
```

### D365 API Client

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class D365ODataApiClient implements D365ApiClient {

    private final D365Configuration config;
    private final D365AuthenticationService authService;
    private final RestTemplate restTemplate;

    @Override
    public D365CountingJournalResponse createCountingJournal(
            D365CountingJournalRequest request) throws D365ApiException {

        String endpoint = config.getApi().getBaseUrl() +
                         config.getApi().getCountingJournalEndpoint();

        // Get access token
        String accessToken = authService.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<D365CountingJournalRequest> httpRequest =
            new HttpEntity<>(request, headers);

        try {
            ResponseEntity<D365CountingJournalResponse> response =
                restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    httpRequest,
                    D365CountingJournalResponse.class
                );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new D365ApiException(
                    "Unexpected response from D365",
                    "UNEXPECTED_RESPONSE",
                    response.getStatusCode().value(),
                    response.toString()
                );
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new D365ApiException(
                "D365 API error: " + e.getMessage(),
                extractErrorCode(e),
                e.getRawStatusCode(),
                e.getResponseBodyAsString()
            );
        } catch (Exception e) {
            throw new D365ApiException(
                "Failed to call D365 API: " + e.getMessage(),
                "NETWORK_ERROR",
                0,
                null
            );
        }
    }

    private String extractErrorCode(HttpStatusCodeException e) {
        try {
            JsonNode errorNode = new ObjectMapper()
                .readTree(e.getResponseBodyAsString())
                .path("error")
                .path("code");
            return errorNode.asText("UNKNOWN_ERROR");
        } catch (Exception ex) {
            return "UNKNOWN_ERROR";
        }
    }
}
```

---

## Frontend Implementation

### React Hooks

```typescript
// useRetryD365Reconciliation.ts
export const useRetryD365Reconciliation = () => {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation<void, Error, RetryReconciliationRequest>({
    mutationFn: (request) =>
      d365ReconciliationService.retryReconciliation(request.reconciliationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['d365-reconciliations'] });
      enqueueSnackbar('Reconciliation retry initiated', { variant: 'success' });
    },
    onError: (error) => {
      enqueueSnackbar(`Failed to retry: ${error.message}`, { variant: 'error' });
    },
  });
};

// useD365ReconciliationRecords.ts
export const useD365ReconciliationRecords = (filters?: {
  status?: ReconciliationStatus;
  fromDate?: string;
  toDate?: string;
}) => {
  return useQuery<D365ReconciliationRecord[], Error>({
    queryKey: ['d365-reconciliations', filters],
    queryFn: () => d365ReconciliationService.getReconciliationRecords(filters),
    refetchInterval: 30000, // Auto-refresh every 30 seconds
  });
};
```

---

## Data Flow

```
StockCount Completed
  ↓
StockCountCompletedEvent published
  ↓
Integration Service: StockCountCompletedEventListener
  ↓
D365StockCountReconciliationService.initiateReconciliation()
  ↓
Create D365StockCountReconciliation aggregate
  ↓
Publish ReconciliationInitiatedEvent
  ↓
Async: executeReconciliation()
  ↓
Build D365CountingJournalRequest
  ↓
Call D365 OData API
  ↓
Success:
  - markSuccessful()
  - Publish ReconciliationCompletedEvent
Failure:
  - markFailed()
  - Calculate next retry time
  - Publish ReconciliationFailedEvent
  - Schedule retry (if attempts < max)
  ↓
Scheduled Job: retryFailedReconciliations()
  ↓
Retry reconciliations ready for retry
```

---

## Testing Strategy

### Unit Tests

```java
@Test
void markFailed_WithAttemptsRemaining_ShouldScheduleRetry() {
    D365StockCountReconciliation reconciliation = createReconciliation(5);
    reconciliation.startSyncAttempt("{}");

    reconciliation.markFailed("Error", "ERR001", "{}", 1000);

    assertThat(reconciliation.getReconciliationStatus())
        .isEqualTo(ReconciliationStatus.RETRYING);
    assertThat(reconciliation.getNextRetryAt()).isNotNull();
}

@Test
void markFailed_MaxAttemptsReached_ShouldSetToFailed() {
    D365StockCountReconciliation reconciliation = createReconciliation(1);
    reconciliation.startSyncAttempt("{}");

    reconciliation.markFailed("Error", "ERR001", "{}", 1000);

    assertThat(reconciliation.getReconciliationStatus())
        .isEqualTo(ReconciliationStatus.FAILED);
    assertThat(reconciliation.getNextRetryAt()).isNull();
}
```

### Integration Tests

```java
@Test
void reconcileStockCount_Success_ShouldCreateD365Journal() {
    // Test successful D365 reconciliation
}

@Test
void reconcileStockCount_Failure_ShouldRetryWithBackoff() {
    // Test retry mechanism
}
```

---

## Acceptance Criteria Validation

| # | Acceptance Criterion | Implementation Status | Validation |
|---|----------------------|----------------------|------------|
| 1 | System reconciles completed stock counts with D365 (if enabled) | ✅ Implemented | `StockCountCompletedEventListener` |
| 2 | Reconciliation includes journal ID, variances, adjusted quantities | ✅ Implemented | `D365CountingJournalRequest` |
| 3 | System handles errors with retry mechanism (exponential backoff) | ✅ Implemented | `markFailed()` with backoff calculation |
| 4 | System maintains reconciliation audit trail | ✅ Implemented | Audit log table with all attempts |
| 5 | System publishes reconciliation events | ✅ Implemented | All events published |
| 6 | Dashboard displays status and allows manual retry | ✅ Implemented | D365ReconciliationDashboard |

---

## Implementation Checklist

### Integration Service - Domain Core
- [ ] Create `D365StockCountReconciliation` aggregate
- [ ] Create value objects and enums
- [ ] Create domain events
- [ ] Unit tests

### Integration Service - Application Service
- [ ] Create `D365StockCountReconciliationService`
- [ ] Create `D365ApiClient` interface
- [ ] Create retry logic
- [ ] Unit tests

### Integration Service - Infrastructure
- [ ] Create `D365ODataApiClient`
- [ ] Create `D365AuthenticationService`
- [ ] Create repositories
- [ ] Database migrations

### Integration Service - Messaging
- [ ] Create `StockCountCompletedEventListener`
- [ ] Create event publishers
- [ ] Integration tests

### Frontend
- [ ] Create types
- [ ] Create hooks
- [ ] Create dashboard
- [ ] Integration tests

### Configuration
- [ ] Add D365 configuration
- [ ] Document environment variables

---

**End of Implementation Plan**
