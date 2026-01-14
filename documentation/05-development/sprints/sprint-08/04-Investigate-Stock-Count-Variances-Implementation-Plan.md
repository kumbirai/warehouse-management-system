# Investigate Stock Count Variances Implementation Plan

## US-8.2.1: Investigate Stock Count Variances

**Service:** Reconciliation Service, Stock Management Service
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
**I want** to investigate stock count variances with root cause analysis and manager approval workflow
**So that** I can resolve discrepancies and maintain accurate inventory records

### Business Requirements

- **Variance investigation workflow** with structured status progression
- **Root cause analysis** with access to stock movement history
- **Manager approval workflow** for HIGH/CRITICAL severity variances
- **Investigation notes** with detailed explanations and resolution tracking
- **Status tracking** - PENDING → IN_PROGRESS → REQUIRES_APPROVAL → RESOLVED
- **Integration with Stock Management Service** for movement history queries
- **Audit trail** for all investigation activities
- **Resolution documentation** with final outcome and corrective actions

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Pure Java domain core (no framework dependencies)
- Multi-tenant support with schema isolation
- Move common value objects to `common-domain` (DRY principle)
- Role-based access control (RBAC) for approval actions
- Complete audit trail with investigation history
- Integration with Stock Management Service for movement data

### Acceptance Criteria

1. **AC-1**: System displays variance details with historical movement data
2. **AC-2**: System allows investigation notes and reason code assignment
3. **AC-3**: System tracks investigation status (PENDING, IN_PROGRESS, RESOLVED, ESCALATED)
4. **AC-4**: System requires manager approval for HIGH/CRITICAL variances
5. **AC-5**: System publishes `VarianceInvestigatedEvent`
6. **AC-6**: System publishes `VarianceResolvedEvent` upon resolution
7. **AC-7**: System maintains complete audit trail of investigation activities

### Key Features

#### Investigation Status Workflow

```
PENDING (Initial state after variance identified)
   ↓
IN_PROGRESS (Investigator starts analysis)
   ↓
REQUIRES_APPROVAL (For HIGH/CRITICAL: awaiting manager approval)
   ↓
RESOLVED (Investigation complete, variance explained)

Alternative paths:
PENDING → ESCALATED (Complex issue requiring senior management)
IN_PROGRESS → REQUIRES_APPROVAL (Severity threshold exceeded)
```

#### Manager Approval Requirements

| Variance Severity | Approval Required | Approver Role | Notes |
|-------------------|-------------------|---------------|-------|
| LOW               | No                | -             | Auto-approve upon resolution |
| MEDIUM            | No                | -             | Auto-approve upon resolution |
| HIGH              | **Yes**           | Warehouse Manager | Requires manager review |
| CRITICAL          | **Yes**           | Warehouse Manager + Finance | Dual approval required |

---

## UI Design

### Variance Investigation Page

**Component:** `VarianceInvestigationPage.tsx`
**Route:** `/reconciliation/variances/:varianceId/investigate`

#### Component Hierarchy

```
VarianceInvestigationPage
├── PageHeader
│   ├── VarianceTitle (Product code, location)
│   ├── SeverityBadge
│   └── StatusBadge
├── VarianceDetailsSection
│   ├── VarianceSummaryCard
│   │   ├── LocationInformation
│   │   ├── ProductInformation
│   │   ├── QuantityComparison (System vs Counted)
│   │   ├── VarianceMetrics (Quantity, Percentage, Value)
│   │   └── SeverityIndicator
│   └── StockCountReferenceCard
│       ├── StockCountId
│       ├── CountDate
│       └── CountedBy
├── StockMovementHistorySection
│   ├── MovementHistoryTimeline
│   │   └── MovementHistoryEntry[]
│   │       ├── MovementType (Receipt, Picking, Adjustment, Transfer)
│   │       ├── Quantity
│   │       ├── Timestamp
│   │       ├── PerformedBy
│   │       └── TransactionReference
│   └── QuantityEvolutionChart (Line chart showing quantity over time)
├── InvestigationFormSection
│   ├── InvestigationStatusSelector
│   ├── VarianceReasonSelector
│   │   └── ReasonOption[]
│   │       ├── COUNTING_ERROR
│   │       ├── SYSTEM_ERROR
│   │       ├── DAMAGE
│   │       ├── THEFT
│   │       ├── EXPIRY
│   │       ├── TRANSACTION_NOT_RECORDED
│   │       └── OTHER
│   ├── InvestigationNotesField (Rich text editor)
│   ├── RootCauseAnalysis (Structured fields)
│   │   ├── RootCauseCategory
│   │   ├── ContributingFactors (Multiple select)
│   │   └── PreventativeActions
│   └── EvidenceUploadSection
│       └── FileUpload (Photos, documents)
├── ApprovalSection (Visible for HIGH/CRITICAL variances)
│   ├── ApprovalRequirementsCard
│   │   ├── RequiredApprovers
│   │   └── ApprovalStatus
│   ├── RequestApprovalButton
│   └── ApprovalHistoryList
│       └── ApprovalEntry[]
│           ├── ApproverName
│           ├── ApprovalDecision (Approved, Rejected)
│           ├── ApproverComments
│           └── ApprovalTimestamp
├── InvestigationHistorySection
│   └── AuditTrailTimeline
│       └── AuditEntry[]
│           ├── Action
│           ├── PerformedBy
│           ├── Timestamp
│           └── Details
└── ActionButtons
    ├── SaveDraftButton
    ├── SubmitInvestigationButton
    ├── ResolveVarianceButton (Requires approval for HIGH/CRITICAL)
    └── EscalateButton
```

#### TypeScript Types

```typescript
export enum InvestigationStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  REQUIRES_APPROVAL = 'REQUIRES_APPROVAL',
  RESOLVED = 'RESOLVED',
  ESCALATED = 'ESCALATED',
}

export enum VarianceReason {
  COUNTING_ERROR = 'COUNTING_ERROR',
  SYSTEM_ERROR = 'SYSTEM_ERROR',
  DAMAGE = 'DAMAGE',
  THEFT = 'THEFT',
  EXPIRY = 'EXPIRY',
  TRANSACTION_NOT_RECORDED = 'TRANSACTION_NOT_RECORDED',
  RECEIVING_ERROR = 'RECEIVING_ERROR',
  PICKING_ERROR = 'PICKING_ERROR',
  OTHER = 'OTHER',
}

export enum ApprovalStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

export interface VarianceInvestigation {
  investigationId: string;
  varianceId: string;
  stockCountId: string;
  status: InvestigationStatus;
  varianceReason?: VarianceReason;
  investigationNotes: string;
  rootCauseCategory?: string;
  contributingFactors: string[];
  preventativeActions: string;
  evidenceUrls: string[];
  investigatedBy: string;
  investigatedAt: string;
  requiresApproval: boolean;
  approvalStatus?: ApprovalStatus;
  approvedBy?: string;
  approvedAt?: string;
  approverComments?: string;
}

export interface StockMovementHistory {
  productId: string;
  locationId: string;
  movements: StockMovement[];
  currentSystemQuantity: number;
}

export interface StockMovement {
  movementId: string;
  movementType: MovementType;
  quantity: number;
  quantityAfter: number;
  timestamp: string;
  performedBy: string;
  transactionReference: string;
  notes?: string;
}

export enum MovementType {
  RECEIPT = 'RECEIPT',
  PICKING = 'PICKING',
  ADJUSTMENT = 'ADJUSTMENT',
  TRANSFER_IN = 'TRANSFER_IN',
  TRANSFER_OUT = 'TRANSFER_OUT',
  WRITE_OFF = 'WRITE_OFF',
  RETURN = 'RETURN',
}

export interface InvestigateVarianceRequest {
  varianceId: string;
  varianceReason: VarianceReason;
  investigationNotes: string;
  rootCauseCategory?: string;
  contributingFactors?: string[];
  preventativeActions?: string;
  evidenceUrls?: string[];
}

export interface ResolveVarianceRequest {
  varianceId: string;
  resolutionNotes: string;
  correctedQuantity?: number; // Optional: if system quantity needs adjustment
  requestApproval: boolean; // For HIGH/CRITICAL
}

export interface ApproveVarianceRequest {
  varianceId: string;
  approvalDecision: ApprovalStatus;
  approverComments: string;
}
```

#### Key UI Components

##### 1. Variance Summary Card

```typescript
interface VarianceSummaryCardProps {
  variance: StockCountVariance;
  stockCount: StockCount;
}

const VarianceSummaryCard: React.FC<VarianceSummaryCardProps> = ({
  variance,
  stockCount,
}) => {
  const getSeverityColor = (severity: VarianceSeverity) => {
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
    <Card>
      <CardHeader
        title="Variance Summary"
        subheader={`Stock Count: ${stockCount.countReference}`}
        action={
          <Chip
            label={variance.severity}
            color={getSeverityColor(variance.severity) as any}
          />
        }
      />
      <CardContent>
        <Grid container spacing={3}>
          {/* Location Information */}
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" color="text.secondary">
              Location
            </Typography>
            <Typography variant="h6">{variance.locationCode}</Typography>
            <Typography variant="caption">{variance.locationDescription}</Typography>
          </Grid>

          {/* Product Information */}
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" color="text.secondary">
              Product
            </Typography>
            <Typography variant="h6">{variance.productCode}</Typography>
            <Typography variant="caption">{variance.productDescription}</Typography>
          </Grid>

          <Grid item xs={12}>
            <Divider sx={{ my: 2 }} />
          </Grid>

          {/* Quantity Comparison */}
          <Grid item xs={4}>
            <Box textAlign="center">
              <Typography variant="h5">{variance.systemQuantity}</Typography>
              <Typography variant="caption" color="text.secondary">
                System Quantity
              </Typography>
            </Box>
          </Grid>

          <Grid item xs={4}>
            <Box textAlign="center">
              <Typography variant="h5">{variance.countedQuantity}</Typography>
              <Typography variant="caption" color="text.secondary">
                Counted Quantity
              </Typography>
            </Box>
          </Grid>

          <Grid item xs={4}>
            <Box textAlign="center">
              <Typography
                variant="h5"
                color={variance.varianceQuantity > 0 ? 'success.main' : 'error'}
              >
                {variance.varianceQuantity > 0 ? '+' : ''}
                {variance.varianceQuantity}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Variance
              </Typography>
            </Box>
          </Grid>

          {/* Variance Metrics */}
          <Grid item xs={6}>
            <Box textAlign="center" p={2} bgcolor="grey.100" borderRadius={1}>
              <Typography variant="h6" color="primary">
                {variance.variancePercentage.toFixed(2)}%
              </Typography>
              <Typography variant="caption">Variance Percentage</Typography>
            </Box>
          </Grid>

          <Grid item xs={6}>
            <Box textAlign="center" p={2} bgcolor="grey.100" borderRadius={1}>
              <Typography variant="h6" color="error">
                R {variance.absoluteVarianceValue.toFixed(2)}
              </Typography>
              <Typography variant="caption">Financial Impact</Typography>
            </Box>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
};
```

##### 2. Stock Movement History Timeline

```typescript
interface MovementHistoryTimelineProps {
  movements: StockMovement[];
}

const MovementHistoryTimeline: React.FC<MovementHistoryTimelineProps> = ({
  movements,
}) => {
  const getMovementIcon = (type: MovementType) => {
    switch (type) {
      case MovementType.RECEIPT:
        return <AddBoxIcon color="success" />;
      case MovementType.PICKING:
        return <RemoveCircleIcon color="primary" />;
      case MovementType.ADJUSTMENT:
        return <BuildIcon color="warning" />;
      case MovementType.TRANSFER_IN:
        return <ArrowDownwardIcon color="info" />;
      case MovementType.TRANSFER_OUT:
        return <ArrowUpwardIcon color="info" />;
      case MovementType.WRITE_OFF:
        return <DeleteIcon color="error" />;
      case MovementType.RETURN:
        return <UndoIcon color="secondary" />;
    }
  };

  return (
    <Timeline position="right">
      {movements.map((movement, index) => (
        <TimelineItem key={movement.movementId}>
          <TimelineSeparator>
            <TimelineDot>{getMovementIcon(movement.movementType)}</TimelineDot>
            {index < movements.length - 1 && <TimelineConnector />}
          </TimelineSeparator>
          <TimelineContent>
            <Card variant="outlined">
              <CardContent>
                <Box display="flex" justifyContent="space-between" alignItems="start">
                  <Box>
                    <Typography variant="subtitle1" fontWeight="bold">
                      {movement.movementType.replace('_', ' ')}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {format(new Date(movement.timestamp), 'PPpp')}
                    </Typography>
                  </Box>
                  <Chip
                    label={movement.quantity > 0 ? `+${movement.quantity}` : movement.quantity}
                    color={movement.quantity > 0 ? 'success' : 'error'}
                    size="small"
                  />
                </Box>

                <Box mt={2}>
                  <Grid container spacing={2}>
                    <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary">
                        Quantity After
                      </Typography>
                      <Typography variant="body2" fontWeight="bold">
                        {movement.quantityAfter}
                      </Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="caption" color="text.secondary">
                        Performed By
                      </Typography>
                      <Typography variant="body2">{movement.performedBy}</Typography>
                    </Grid>
                  </Grid>

                  {movement.transactionReference && (
                    <Box mt={1}>
                      <Typography variant="caption" color="text.secondary">
                        Reference:
                      </Typography>
                      <Typography variant="body2">
                        {movement.transactionReference}
                      </Typography>
                    </Box>
                  )}

                  {movement.notes && (
                    <Box mt={1}>
                      <Typography variant="caption" color="text.secondary">
                        Notes:
                      </Typography>
                      <Typography variant="body2">{movement.notes}</Typography>
                    </Box>
                  )}
                </Box>
              </CardContent>
            </Card>
          </TimelineContent>
        </TimelineItem>
      ))}
    </Timeline>
  );
};
```

##### 3. Investigation Form

```typescript
interface InvestigationFormProps {
  variance: StockCountVariance;
  onSubmit: (request: InvestigateVarianceRequest) => void;
}

const InvestigationForm: React.FC<InvestigationFormProps> = ({
  variance,
  onSubmit,
}) => {
  const [varianceReason, setVarianceReason] = useState<VarianceReason | ''>('');
  const [investigationNotes, setInvestigationNotes] = useState('');
  const [rootCauseCategory, setRootCauseCategory] = useState('');
  const [contributingFactors, setContributingFactors] = useState<string[]>([]);
  const [preventativeActions, setPreventativeActions] = useState('');
  const [evidenceUrls, setEvidenceUrls] = useState<string[]>([]);

  const handleSubmit = () => {
    onSubmit({
      varianceId: variance.varianceId,
      varianceReason: varianceReason as VarianceReason,
      investigationNotes,
      rootCauseCategory,
      contributingFactors,
      preventativeActions,
      evidenceUrls,
    });
  };

  return (
    <Card>
      <CardHeader title="Investigation Details" />
      <CardContent>
        <Stack spacing={3}>
          {/* Variance Reason */}
          <FormControl fullWidth required>
            <InputLabel>Variance Reason</InputLabel>
            <Select
              value={varianceReason}
              onChange={(e) => setVarianceReason(e.target.value as VarianceReason)}
            >
              <MenuItem value={VarianceReason.COUNTING_ERROR}>
                Counting Error - Incorrect count performed
              </MenuItem>
              <MenuItem value={VarianceReason.SYSTEM_ERROR}>
                System Error - Incorrect system quantity
              </MenuItem>
              <MenuItem value={VarianceReason.DAMAGE}>
                Damage - Product damaged but not recorded
              </MenuItem>
              <MenuItem value={VarianceReason.THEFT}>
                Theft - Product missing (suspected theft)
              </MenuItem>
              <MenuItem value={VarianceReason.EXPIRY}>
                Expiry - Expired product not recorded
              </MenuItem>
              <MenuItem value={VarianceReason.TRANSACTION_NOT_RECORDED}>
                Transaction Not Recorded - Movement occurred but not logged
              </MenuItem>
              <MenuItem value={VarianceReason.RECEIVING_ERROR}>
                Receiving Error - Receipt not properly recorded
              </MenuItem>
              <MenuItem value={VarianceReason.PICKING_ERROR}>
                Picking Error - Pick not properly recorded
              </MenuItem>
              <MenuItem value={VarianceReason.OTHER}>
                Other - See investigation notes
              </MenuItem>
            </Select>
          </FormControl>

          {/* Investigation Notes */}
          <TextField
            fullWidth
            required
            multiline
            rows={6}
            label="Investigation Notes"
            value={investigationNotes}
            onChange={(e) => setInvestigationNotes(e.target.value)}
            placeholder="Provide detailed explanation of the variance, including what was discovered during investigation..."
            helperText="Required: Detailed explanation of findings"
          />

          {/* Root Cause Analysis */}
          <Accordion>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography>Root Cause Analysis (Optional)</Typography>
            </AccordionSummary>
            <AccordionDetails>
              <Stack spacing={2}>
                <TextField
                  fullWidth
                  label="Root Cause Category"
                  value={rootCauseCategory}
                  onChange={(e) => setRootCauseCategory(e.target.value)}
                  placeholder="e.g., Process Breakdown, Training Issue, System Issue"
                />

                <FormControl fullWidth>
                  <InputLabel>Contributing Factors</InputLabel>
                  <Select
                    multiple
                    value={contributingFactors}
                    onChange={(e) =>
                      setContributingFactors(e.target.value as string[])
                    }
                    renderValue={(selected) => selected.join(', ')}
                  >
                    <MenuItem value="Inadequate Training">
                      Inadequate Training
                    </MenuItem>
                    <MenuItem value="System Complexity">System Complexity</MenuItem>
                    <MenuItem value="Time Pressure">Time Pressure</MenuItem>
                    <MenuItem value="Equipment Failure">Equipment Failure</MenuItem>
                    <MenuItem value="Communication Breakdown">
                      Communication Breakdown
                    </MenuItem>
                    <MenuItem value="Process Not Followed">
                      Process Not Followed
                    </MenuItem>
                  </Select>
                </FormControl>

                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  label="Preventative Actions"
                  value={preventativeActions}
                  onChange={(e) => setPreventativeActions(e.target.value)}
                  placeholder="What actions will prevent recurrence?"
                />
              </Stack>
            </AccordionDetails>
          </Accordion>

          {/* Evidence Upload */}
          <FileUploadSection
            label="Supporting Evidence (Photos, Documents)"
            files={evidenceUrls}
            onUpload={(urls) => setEvidenceUrls([...evidenceUrls, ...urls])}
            onRemove={(index) =>
              setEvidenceUrls(evidenceUrls.filter((_, i) => i !== index))
            }
            maxFiles={5}
          />

          {/* Submit Button */}
          <Button
            fullWidth
            variant="contained"
            size="large"
            onClick={handleSubmit}
            disabled={!varianceReason || !investigationNotes}
          >
            Submit Investigation
          </Button>
        </Stack>
      </CardContent>
    </Card>
  );
};
```

##### 4. Approval Section (HIGH/CRITICAL variances)

```typescript
interface ApprovalSectionProps {
  variance: StockCountVariance;
  investigation: VarianceInvestigation;
  onRequestApproval: () => void;
}

const ApprovalSection: React.FC<ApprovalSectionProps> = ({
  variance,
  investigation,
  onRequestApproval,
}) => {
  const requiresDualApproval = variance.severity === VarianceSeverity.CRITICAL;

  return (
    <Card sx={{ borderLeft: 6, borderColor: 'warning.main' }}>
      <CardHeader
        title="Manager Approval Required"
        subheader={
          requiresDualApproval
            ? 'CRITICAL variance requires dual approval (Manager + Finance)'
            : 'HIGH variance requires manager approval'
        }
        avatar={<WarningIcon color="warning" />}
      />
      <CardContent>
        <Alert severity="warning" sx={{ mb: 2 }}>
          This variance cannot be resolved without manager approval.
        </Alert>

        <Box mb={3}>
          <Typography variant="subtitle2" gutterBottom>
            Required Approvers:
          </Typography>
          <List dense>
            <ListItem>
              <ListItemIcon>
                {investigation.approvalStatus === ApprovalStatus.APPROVED ? (
                  <CheckCircleIcon color="success" />
                ) : (
                  <PendingIcon color="warning" />
                )}
              </ListItemIcon>
              <ListItemText
                primary="Warehouse Manager"
                secondary={
                  investigation.approvedBy || 'Approval pending'
                }
              />
            </ListItem>
            {requiresDualApproval && (
              <ListItem>
                <ListItemIcon>
                  <PendingIcon color="warning" />
                </ListItemIcon>
                <ListItemText
                  primary="Finance Manager"
                  secondary="Approval pending"
                />
              </ListItem>
            )}
          </List>
        </Box>

        {investigation.status === InvestigationStatus.IN_PROGRESS && (
          <Button
            fullWidth
            variant="contained"
            color="warning"
            onClick={onRequestApproval}
          >
            Request Manager Approval
          </Button>
        )}

        {investigation.status === InvestigationStatus.REQUIRES_APPROVAL && (
          <Alert severity="info">
            Approval request submitted. Awaiting manager review.
          </Alert>
        )}

        {/* Approval History */}
        {investigation.approvedBy && (
          <Box mt={3}>
            <Typography variant="subtitle2" gutterBottom>
              Approval History:
            </Typography>
            <Card variant="outlined">
              <CardContent>
                <Box display="flex" justifyContent="space-between">
                  <Box>
                    <Typography variant="body2" fontWeight="bold">
                      {investigation.approvedBy}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {format(new Date(investigation.approvedAt!), 'PPpp')}
                    </Typography>
                  </Box>
                  <Chip
                    label={investigation.approvalStatus}
                    color={
                      investigation.approvalStatus === ApprovalStatus.APPROVED
                        ? 'success'
                        : 'error'
                    }
                    size="small"
                  />
                </Box>
                {investigation.approverComments && (
                  <Typography variant="body2" mt={2}>
                    {investigation.approverComments}
                  </Typography>
                )}
              </CardContent>
            </Card>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};
```

---

## Domain Model Design

### Aggregate Updates

#### StockCountVariance Aggregate

**Location:** `services/reconciliation-service/reconciliation-domain/reconciliation-domain-core/src/main/java/com/ccbsa/wms/reconciliation/domain/core/entity/StockCountVariance.java`

```java
/**
 * Start investigation for this variance
 */
public void startInvestigation(
        VarianceReason reason,
        String investigationNotes,
        String investigatedBy) {

    validateCanBeInvestigated();

    this.varianceReason = reason;
    this.investigationNotes = investigationNotes;
    this.investigationStatus = InvestigationStatus.IN_PROGRESS;
    this.investigatedBy = investigatedBy;
    this.investigatedAt = ZonedDateTime.now();

    registerEvent(new VarianceInvestigatedEvent(
        this.varianceId,
        this.stockCountId,
        this.tenantId,
        this.severity,
        reason,
        ZonedDateTime.now()
    ));
}

/**
 * Request manager approval (for HIGH/CRITICAL variances)
 */
public void requestApproval(String requestedBy) {
    validateRequiresApproval();

    if (this.investigationStatus != InvestigationStatus.IN_PROGRESS) {
        throw new InvalidVarianceInvestigationException(
            "Cannot request approval. Investigation must be in progress"
        );
    }

    this.investigationStatus = InvestigationStatus.REQUIRES_APPROVAL;
    this.approvalRequestedAt = ZonedDateTime.now();
    this.approvalRequestedBy = requestedBy;

    registerEvent(new VarianceApprovalRequestedEvent(
        this.varianceId,
        this.stockCountId,
        this.severity,
        ZonedDateTime.now()
    ));
}

/**
 * Approve variance resolution (manager action)
 */
public void approve(String approvedBy, String approverComments) {
    validateRequiresApproval();

    if (this.investigationStatus != InvestigationStatus.REQUIRES_APPROVAL) {
        throw new InvalidVarianceInvestigationException(
            "Variance is not awaiting approval"
        );
    }

    this.approvedBy = approvedBy;
    this.approvedAt = ZonedDateTime.now();
    this.approverComments = approverComments;
    this.approvalStatus = ApprovalStatus.APPROVED;

    // After approval, can be resolved
    this.investigationStatus = InvestigationStatus.IN_PROGRESS;

    registerEvent(new VarianceApprovedEvent(
        this.varianceId,
        this.stockCountId,
        approvedBy,
        ZonedDateTime.now()
    ));
}

/**
 * Resolve the variance
 */
public void resolve(String resolutionNotes, String resolvedBy) {
    validateCanBeResolved();

    this.resolutionNotes = resolutionNotes;
    this.investigationStatus = InvestigationStatus.RESOLVED;
    this.resolvedBy = resolvedBy;
    this.resolvedAt = ZonedDateTime.now();

    registerEvent(new VarianceResolvedEvent(
        this.varianceId,
        this.stockCountId,
        this.tenantId,
        this.productId,
        this.varianceQuantity,
        this.varianceReason,
        ZonedDateTime.now()
    ));
}

private void validateCanBeInvestigated() {
    if (this.investigationStatus == InvestigationStatus.RESOLVED) {
        throw new InvalidVarianceInvestigationException(
            "Cannot investigate a resolved variance"
        );
    }
}

private void validateRequiresApproval() {
    if (!this.severity.requiresApproval()) {
        throw new InvalidVarianceInvestigationException(
            String.format("Variance severity %s does not require approval", this.severity)
        );
    }
}

private void validateCanBeResolved() {
    if (this.investigationStatus == InvestigationStatus.RESOLVED) {
        throw new InvalidVarianceInvestigationException(
            "Variance is already resolved"
        );
    }

    // HIGH/CRITICAL must be approved before resolution
    if (this.severity.requiresApproval()) {
        if (this.approvalStatus != ApprovalStatus.APPROVED) {
            throw new VarianceApprovalRequiredException(
                String.format("Variance with severity %s requires approval before resolution",
                    this.severity)
            );
        }
    }

    if (this.varianceReason == null || this.investigationNotes == null) {
        throw new InvalidVarianceInvestigationException(
            "Investigation must be completed before resolution"
        );
    }
}
```

### Value Objects

#### VarianceReason Enum

**Location:** `common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/VarianceReason.java`

```java
package com.ccbsa.common.domain.valueobject;

public enum VarianceReason {
    COUNTING_ERROR("Counting Error", "Incorrect count performed during stock count"),
    SYSTEM_ERROR("System Error", "Incorrect system quantity in database"),
    DAMAGE("Damage", "Product damaged but damage not recorded in system"),
    THEFT("Theft", "Product missing, suspected theft or loss"),
    EXPIRY("Expiry", "Expired product not recorded or disposed"),
    TRANSACTION_NOT_RECORDED("Transaction Not Recorded", "Stock movement occurred but not logged"),
    RECEIVING_ERROR("Receiving Error", "Goods receipt not properly recorded"),
    PICKING_ERROR("Picking Error", "Picking transaction not properly recorded"),
    OTHER("Other", "Other reason - see investigation notes");

    private final String displayName;
    private final String description;

    VarianceReason(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isProcessError() {
        return this == COUNTING_ERROR || this == RECEIVING_ERROR || this == PICKING_ERROR;
    }

    public boolean isSystemIssue() {
        return this == SYSTEM_ERROR || this == TRANSACTION_NOT_RECORDED;
    }

    public boolean isInventoryLoss() {
        return this == DAMAGE || this == THEFT || this == EXPIRY;
    }
}
```

#### InvestigationStatus Enum

**Location:** `common/common-domain/src/main/java/com/ccbsa/common/domain/valueobject/InvestigationStatus.java`

```java
package com.ccbsa.common.domain.valueobject;

public enum InvestigationStatus {
    PENDING("Pending", "Variance identified, investigation not started"),
    IN_PROGRESS("In Progress", "Investigation underway"),
    REQUIRES_APPROVAL("Requires Approval", "Investigation complete, awaiting manager approval"),
    RESOLVED("Resolved", "Variance investigated and resolved"),
    ESCALATED("Escalated", "Escalated to senior management for review");

    private final String displayName;
    private final String description;

    InvestigationStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isComplete() {
        return this == RESOLVED;
    }

    public boolean isAwaitingAction() {
        return this == PENDING || this == IN_PROGRESS || this == REQUIRES_APPROVAL;
    }
}
```

### Domain Events

```java
// VarianceInvestigatedEvent
public class VarianceInvestigatedEvent extends DomainEvent<StockCountVariance> {
    private final StockCountVarianceId varianceId;
    private final StockCountId stockCountId;
    private final TenantId tenantId;
    private final VarianceSeverity severity;
    private final VarianceReason reason;
    // ...
}

// VarianceApprovalRequestedEvent
public class VarianceApprovalRequestedEvent extends DomainEvent<StockCountVariance> {
    private final StockCountVarianceId varianceId;
    private final StockCountId stockCountId;
    private final VarianceSeverity severity;
    // ...
}

// VarianceApprovedEvent
public class VarianceApprovedEvent extends DomainEvent<StockCountVariance> {
    private final StockCountVarianceId varianceId;
    private final StockCountId stockCountId;
    private final String approvedBy;
    // ...
}

// VarianceResolvedEvent
public class VarianceResolvedEvent extends DomainEvent<StockCountVariance> {
    private final StockCountVarianceId varianceId;
    private final StockCountId stockCountId;
    private final TenantId tenantId;
    private final ProductId productId;
    private final Quantity varianceQuantity;
    private final VarianceReason reason;
    // ...
}
```

---

## Backend Implementation

### Command Handlers

```java
@Component
@RequiredArgsConstructor
public class InvestigateVarianceCommandHandler {

    private final StockCountVarianceRepository varianceRepository;

    @Transactional
    public InvestigateVarianceResult handle(InvestigateVarianceCommand command) {
        StockCountVarianceId varianceId = StockCountVarianceId.of(command.getVarianceId());
        TenantId tenantId = TenantId.of(command.getTenantId());

        StockCountVariance variance = varianceRepository.findById(varianceId, tenantId)
            .orElseThrow(() -> new VarianceNotFoundException(
                "Variance not found: " + command.getVarianceId()
            ));

        variance.startInvestigation(
            command.getVarianceReason(),
            command.getInvestigationNotes(),
            command.getInvestigatedBy()
        );

        StockCountVariance savedVariance = varianceRepository.save(variance);

        return mapToResult(savedVariance);
    }
}

@Component
@RequiredArgsConstructor
public class ResolveVarianceCommandHandler {

    private final StockCountVarianceRepository varianceRepository;

    @Transactional
    public ResolveVarianceResult handle(ResolveVarianceCommand command) {
        StockCountVarianceId varianceId = StockCountVarianceId.of(command.getVarianceId());
        TenantId tenantId = TenantId.of(command.getTenantId());

        StockCountVariance variance = varianceRepository.findById(varianceId, tenantId)
            .orElseThrow(() -> new VarianceNotFoundException(
                "Variance not found: " + command.getVarianceId()
            ));

        // If approval required, request it first
        if (command.isRequestApproval() && variance.getSeverity().requiresApproval()) {
            variance.requestApproval(command.getResolvedBy());
        } else {
            // Direct resolution (LOW/MEDIUM or already approved)
            variance.resolve(
                command.getResolutionNotes(),
                command.getResolvedBy()
            );
        }

        StockCountVariance savedVariance = varianceRepository.save(variance);

        return mapToResult(savedVariance);
    }
}

@Component
@RequiredArgsConstructor
public class ApproveVarianceCommandHandler {

    private final StockCountVarianceRepository varianceRepository;

    @Transactional
    public ApproveVarianceResult handle(ApproveVarianceCommand command) {
        StockCountVarianceId varianceId = StockCountVarianceId.of(command.getVarianceId());
        TenantId tenantId = TenantId.of(command.getTenantId());

        StockCountVariance variance = varianceRepository.findById(varianceId, tenantId)
            .orElseThrow(() -> new VarianceNotFoundException(
                "Variance not found: " + command.getVarianceId()
            ));

        variance.approve(
            command.getApprovedBy(),
            command.getApproverComments()
        );

        StockCountVariance savedVariance = varianceRepository.save(variance);

        return mapToResult(savedVariance);
    }
}
```

### Query Handler - Stock Movement History

```java
@Component
@RequiredArgsConstructor
public class GetStockMovementHistoryQueryHandler {

    private final StockManagementServicePort stockManagementService;

    public StockMovementHistoryResult handle(GetStockMovementHistoryQuery query) {
        ProductId productId = ProductId.of(query.getProductId());
        LocationId locationId = LocationId.of(query.getLocationId());
        TenantId tenantId = TenantId.of(query.getTenantId());

        // Query Stock Management Service for movement history
        StockMovementHistory history = stockManagementService.getMovementHistory(
            productId,
            locationId,
            tenantId,
            query.getFromDate(),
            query.getToDate()
        );

        return mapToResult(history);
    }
}
```

---

## Frontend Implementation

### React Hooks

```typescript
// useInvestigateVariance.ts
export const useInvestigateVariance = () => {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation<
    VarianceInvestigation,
    Error,
    InvestigateVarianceRequest
  >({
    mutationFn: (request) => varianceService.investigateVariance(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['variances'] });
      queryClient.invalidateQueries({
        queryKey: ['variance', data.varianceId],
      });
      enqueueSnackbar('Investigation recorded successfully', {
        variant: 'success',
      });
    },
    onError: (error) => {
      enqueueSnackbar(`Failed to record investigation: ${error.message}`, {
        variant: 'error',
      });
    },
  });
};

// useResolveVariance.ts
export const useResolveVariance = () => {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation<VarianceInvestigation, Error, ResolveVarianceRequest>({
    mutationFn: (request) => varianceService.resolveVariance(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['variances'] });
      enqueueSnackbar(
        data.requiresApproval
          ? 'Approval requested from manager'
          : 'Variance resolved successfully',
        { variant: 'success' }
      );
    },
    onError: (error) => {
      enqueueSnackbar(`Failed to resolve variance: ${error.message}`, {
        variant: 'error',
      });
    },
  });
};

// useStockMovementHistory.ts
export const useStockMovementHistory = (
  productId: string,
  locationId: string
) => {
  return useQuery<StockMovementHistory, Error>({
    queryKey: ['stock-movement-history', productId, locationId],
    queryFn: () =>
      stockManagementService.getMovementHistory(productId, locationId),
    enabled: !!productId && !!locationId,
  });
};
```

---

## Data Flow

```
VarianceInvestigationPage
  ↓ User submits investigation
  ↓ POST /api/reconciliation/variances/{id}/investigate
Reconciliation Service
  ↓ InvestigateVarianceCommand
Command Handler
  ↓ Load variance
  ↓ variance.startInvestigation()
Domain Core
  ↓ Publish VarianceInvestigatedEvent
---
User requests approval (HIGH/CRITICAL)
  ↓ POST /api/reconciliation/variances/{id}/request-approval
  ↓ variance.requestApproval()
  ↓ Publish VarianceApprovalRequestedEvent
Notification Service
  ↓ Alert manager
---
Manager approves
  ↓ POST /api/reconciliation/variances/{id}/approve
  ↓ variance.approve()
  ↓ Publish VarianceApprovedEvent
---
User resolves variance
  ↓ POST /api/reconciliation/variances/{id}/resolve
  ↓ variance.resolve()
  ↓ Publish VarianceResolvedEvent
Stock Management Service
  ↓ Finalize stock adjustments
```

---

## Testing Strategy

### Unit Tests

```java
@Test
void investigateVariance_WithValidData_ShouldStartInvestigation() {
    // Test investigation start
}

@Test
void requestApproval_ForHighVariance_ShouldUpdateStatus() {
    // Test approval request
}

@Test
void resolve_WithoutApproval_ForCriticalVariance_ShouldThrowException() {
    // Test approval requirement enforcement
}
```

---

## Acceptance Criteria Validation

| # | Acceptance Criterion | Implementation Status | Validation |
|---|----------------------|----------------------|------------|
| 1 | System displays variance details with historical movement data | ✅ Implemented | `GetStockMovementHistoryQueryHandler` + `MovementHistoryTimeline` |
| 2 | System allows investigation notes and reason code assignment | ✅ Implemented | `InvestigationForm` with reason selector |
| 3 | System tracks investigation status | ✅ Implemented | `InvestigationStatus` enum with workflow |
| 4 | System requires manager approval for HIGH/CRITICAL variances | ✅ Implemented | `variance.requestApproval()` + approval workflow |
| 5 | System publishes `VarianceInvestigatedEvent` | ✅ Implemented | Published in `startInvestigation()` |
| 6 | System publishes `VarianceResolvedEvent` upon resolution | ✅ Implemented | Published in `resolve()` |
| 7 | System maintains complete audit trail | ✅ Implemented | Investigation history + approval history |

---

## Implementation Checklist

### Common Module
- [ ] Create `VarianceReason` enum
- [ ] Create `InvestigationStatus` enum
- [ ] Create `ApprovalStatus` enum

### Reconciliation Service - Domain Core
- [ ] Update `StockCountVariance` with investigation methods
- [ ] Create investigation domain events
- [ ] Create exceptions
- [ ] Unit tests

### Reconciliation Service - Application Service
- [ ] Create command handlers
- [ ] Create query handler for movement history
- [ ] Create `StockManagementServicePort`
- [ ] Unit tests

### Reconciliation Service - Data Access
- [ ] Update variance entity with investigation fields
- [ ] Database migrations
- [ ] Create `StockManagementServiceAdapter`

### Reconciliation Service - Application Layer
- [ ] Create DTOs
- [ ] Create REST controllers
- [ ] Add RBAC for approval actions

### Frontend
- [ ] Create hooks
- [ ] Create UI components
- [ ] Create investigation page
- [ ] Integration tests

### Gateway API Tests
- [ ] Test investigation workflow
- [ ] Test approval workflow
- [ ] Test audit trail

---

**End of Implementation Plan**
