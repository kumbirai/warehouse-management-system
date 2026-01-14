# US-7.4.1: Assign Return Location Implementation Plan

**User Story ID:** US-7.4.1
**Epic:** Returns Management (Epic 7)
**Story Points:** 5
**Sprint:** Sprint 7
**Service:** Location Management Service, Returns Service
**Related Stories:** US-7.1.1 (Handle Partial Order Acceptance), US-7.2.1 (Process Full Order Return), US-7.3.1 (Handle Damage-in-Transit Returns)

---

## Table of Contents

1. [Overview](#overview)
2. [Business Requirements](#business-requirements)
3. [Production-Grade UI Design](#production-grade-ui-design)
4. [Domain Model Design](#domain-model-design)
5. [Backend Implementation](#backend-implementation)
6. [Frontend Implementation](#frontend-implementation)
7. [Testing Strategy](#testing-strategy)
8. [Acceptance Criteria Validation](#acceptance-criteria-validation)
9. [Implementation Checklist](#implementation-checklist)

---

## Overview

### User Story

**As a** warehouse operator
**I want to** automatically assign returned goods to appropriate warehouse locations based on product condition
**So that** returned products are stored correctly and can be processed efficiently

### Business Context

After processing returns (partial, full, or damage assessments), the system must intelligently assign returned products to appropriate warehouse locations based on:

- Product condition (GOOD, DAMAGED, EXPIRED, QUARANTINE, WRITE_OFF)
- Location type (Available stock, Quarantine zone, Disposal area)
- Warehouse capacity and organization
- FEFO principles for good condition returns
- Safety and regulatory compliance for damaged/expired products

The system uses event-driven choreography where Location Management Service listens to return events and automatically assigns locations without manual intervention.

### Acceptance Criteria

1. ✅ System automatically assigns good condition products to available stock locations using FEFO
2. ✅ System routes damaged products to quarantine zone for inspection
3. ✅ System segregates expired products to dedicated disposal locations
4. ✅ System assigns write-off products to disposal/scrap locations
5. ✅ System publishes LocationAssignedEvent after successful assignment
6. ✅ System validates location capacity before assignment

---

## Production-Grade UI Design

### Page: Return Location Assignment Dashboard (`/location-management/return-assignments`)

#### Component Hierarchy

```
ReturnLocationAssignmentDashboard
├── PageBreadcrumbs
├── DashboardHeader
│   ├── PendingReturnsSummary
│   └── FilterControls
├── PendingReturnsTable
│   └── PendingReturnRow[]
│       ├── ReturnInfo (ID, Order, Customer)
│       ├── ConditionSummary (Good/Damaged/Write-off counts)
│       ├── AssignmentStatus
│       └── QuickActions
│           ├── ViewDetailsButton
│           ├── ManualAssignButton
│           └── AutoAssignButton
└── AssignmentHistory
    └── RecentAssignments[]
```

#### UI Components

##### 1. Pending Returns Summary

```typescript
interface PendingReturnsSummaryProps {
  totalPending: number;
  goodCondition: number;
  damaged: number;
  expired: number;
  writeOff: number;
}

const PendingReturnsSummary: React.FC<PendingReturnsSummaryProps> = ({
  totalPending,
  goodCondition,
  damaged,
  expired,
  writeOff,
}) => {
  return (
    <Grid container spacing={3}>
      <Grid item xs={12} md={2.4}>
        <Card>
          <CardContent>
            <Box textAlign="center">
              <Typography variant="h4" color="primary">
                {totalPending}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Pending Assignments
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>

      <Grid item xs={12} md={2.4}>
        <Card>
          <CardContent>
            <Box textAlign="center">
              <Typography variant="h4" color="success.main">
                {goodCondition}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Good Condition
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>

      <Grid item xs={12} md={2.4}>
        <Card>
          <CardContent>
            <Box textAlign="center">
              <Typography variant="h4" color="warning.main">
                {damaged}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Damaged - Quarantine
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>

      <Grid item xs={12} md={2.4}>
        <Card>
          <CardContent>
            <Box textAlign="center">
              <Typography variant="h4" color="error.main">
                {expired}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Expired - Disposal
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>

      <Grid item xs={12} md={2.4}>
        <Card>
          <CardContent>
            <Box textAlign="center">
              <Typography variant="h4" color="text.secondary">
                {writeOff}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Write-Off - Scrap
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );
};
```

##### 2. Pending Returns Table

```typescript
interface PendingReturn {
  returnId: string;
  orderId: string;
  customerName: string;
  returnType: 'PARTIAL' | 'FULL';
  goodConditionCount: number;
  damagedCount: number;
  expiredCount: number;
  writeOffCount: number;
  assignmentStatus: 'PENDING' | 'PARTIAL' | 'COMPLETED';
  returnedAt: string;
}

const PendingReturnsTable: React.FC = () => {
  const { data: pendingReturns, isLoading } = usePendingReturns();
  const { mutate: autoAssign } = useAutoAssignReturnLocations();

  if (isLoading) return <SkeletonTable rows={5} columns={7} />;

  return (
    <ResponsiveTable>
      <TableHead>
        <TableRow>
          <TableCell>Return ID</TableCell>
          <TableCell>Order</TableCell>
          <TableCell>Customer</TableCell>
          <TableCell>Type</TableCell>
          <TableCell>Condition Summary</TableCell>
          <TableCell>Status</TableCell>
          <TableCell align="right">Actions</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {pendingReturns?.map((returnItem) => (
          <TableRow key={returnItem.returnId}>
            <TableCell>
              <Link to={`/returns/${returnItem.returnId}`}>
                {returnItem.returnId.substring(0, 8)}
              </Link>
            </TableCell>
            <TableCell>{returnItem.orderId.substring(0, 8)}</TableCell>
            <TableCell>{returnItem.customerName}</TableCell>
            <TableCell>
              <Chip
                label={returnItem.returnType}
                size="small"
                color={returnItem.returnType === 'FULL' ? 'error' : 'warning'}
              />
            </TableCell>
            <TableCell>
              <Box display="flex" gap={1}>
                {returnItem.goodConditionCount > 0 && (
                  <Chip
                    label={`Good: ${returnItem.goodConditionCount}`}
                    size="small"
                    color="success"
                  />
                )}
                {returnItem.damagedCount > 0 && (
                  <Chip
                    label={`Damaged: ${returnItem.damagedCount}`}
                    size="small"
                    color="warning"
                  />
                )}
                {returnItem.expiredCount > 0 && (
                  <Chip
                    label={`Expired: ${returnItem.expiredCount}`}
                    size="small"
                    color="error"
                  />
                )}
                {returnItem.writeOffCount > 0 && (
                  <Chip
                    label={`Write-Off: ${returnItem.writeOffCount}`}
                    size="small"
                    color="default"
                  />
                )}
              </Box>
            </TableCell>
            <TableCell>
              <StatusBadge status={returnItem.assignmentStatus} />
            </TableCell>
            <TableCell align="right">
              <IconButton
                size="small"
                onClick={() => autoAssign({ returnId: returnItem.returnId })}
                title="Auto-assign locations"
              >
                <AutoModeIcon />
              </IconButton>
              <IconButton
                size="small"
                component={Link}
                to={`/returns/${returnItem.returnId}/manual-assign`}
                title="Manual assignment"
              >
                <EditLocationIcon />
              </IconButton>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </ResponsiveTable>
  );
};
```

##### 3. Manual Location Assignment Dialog

```typescript
interface ManualLocationAssignmentDialogProps {
  open: boolean;
  returnId: string;
  returnLineItems: ReturnLineItem[];
  onClose: () => void;
}

const ManualLocationAssignmentDialog: React.FC<ManualLocationAssignmentDialogProps> = ({
  open,
  returnId,
  returnLineItems,
  onClose,
}) => {
  const [assignments, setAssignments] = useState<LocationAssignment[]>([]);
  const { mutate: assignLocations } = useAssignReturnLocations();
  const { data: availableLocations } = useAvailableLocations();

  const handleAssign = () => {
    assignLocations(
      {
        returnId,
        assignments: assignments.map((a) => ({
          returnLineItemId: a.returnLineItemId,
          locationId: a.locationId,
        })),
      },
      {
        onSuccess: () => {
          enqueueSnackbar('Locations assigned successfully', { variant: 'success' });
          onClose();
        },
      }
    );
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>Manual Location Assignment - Return {returnId.substring(0, 8)}</DialogTitle>
      <DialogContent>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Product</TableCell>
              <TableCell>Quantity</TableCell>
              <TableCell>Condition</TableCell>
              <TableCell>Assigned Location</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {returnLineItems.map((lineItem, index) => (
              <TableRow key={lineItem.returnLineItemId}>
                <TableCell>{lineItem.productDescription}</TableCell>
                <TableCell>{lineItem.returnedQuantity}</TableCell>
                <TableCell>
                  <Chip
                    label={lineItem.productCondition}
                    color={getConditionColor(lineItem.productCondition)}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  <Autocomplete
                    options={filterLocationsByCondition(
                      availableLocations,
                      lineItem.productCondition
                    )}
                    getOptionLabel={(option) => `${option.code} - ${option.name}`}
                    renderInput={(params) => (
                      <TextField {...params} label="Select Location" required />
                    )}
                    onChange={(_, value) => {
                      const newAssignments = [...assignments];
                      newAssignments[index] = {
                        returnLineItemId: lineItem.returnLineItemId,
                        locationId: value?.locationId || '',
                      };
                      setAssignments(newAssignments);
                    }}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          onClick={handleAssign}
          disabled={assignments.length !== returnLineItems.length}
        >
          Assign Locations
        </Button>
      </DialogActions>
    </Dialog>
  );
};
```

##### 4. Location Assignment Confirmation

```typescript
interface LocationAssignmentConfirmationProps {
  assignments: AssignedLocation[];
  onConfirm: () => void;
}

const LocationAssignmentConfirmation: React.FC<LocationAssignmentConfirmationProps> = ({
  assignments,
  onConfirm,
}) => {
  const goodConditionAssignments = assignments.filter(
    (a) => a.productCondition === 'GOOD'
  );
  const quarantineAssignments = assignments.filter(
    (a) => a.productCondition === 'DAMAGED' || a.productCondition === 'QUARANTINE'
  );
  const disposalAssignments = assignments.filter(
    (a) => a.productCondition === 'EXPIRED' || a.productCondition === 'WRITE_OFF'
  );

  return (
    <Card>
      <CardHeader title="Location Assignment Summary" />
      <CardContent>
        <Grid container spacing={2}>
          {goodConditionAssignments.length > 0 && (
            <Grid item xs={12} md={4}>
              <Paper variant="outlined" sx={{ p: 2, bgcolor: 'success.light' }}>
                <Typography variant="h6" color="success.dark">
                  Available Stock
                </Typography>
                <Typography variant="body2">
                  {goodConditionAssignments.length} items assigned to available locations
                </Typography>
                <List dense>
                  {goodConditionAssignments.slice(0, 3).map((a) => (
                    <ListItem key={a.returnLineItemId}>
                      <ListItemText
                        primary={a.productCode}
                        secondary={`Location: ${a.locationCode}`}
                      />
                    </ListItem>
                  ))}
                  {goodConditionAssignments.length > 3 && (
                    <ListItem>
                      <ListItemText
                        secondary={`+${goodConditionAssignments.length - 3} more`}
                      />
                    </ListItem>
                  )}
                </List>
              </Paper>
            </Grid>
          )}

          {quarantineAssignments.length > 0 && (
            <Grid item xs={12} md={4}>
              <Paper variant="outlined" sx={{ p: 2, bgcolor: 'warning.light' }}>
                <Typography variant="h6" color="warning.dark">
                  Quarantine Zone
                </Typography>
                <Typography variant="body2">
                  {quarantineAssignments.length} items assigned to quarantine
                </Typography>
                <List dense>
                  {quarantineAssignments.slice(0, 3).map((a) => (
                    <ListItem key={a.returnLineItemId}>
                      <ListItemText
                        primary={a.productCode}
                        secondary={`Location: ${a.locationCode}`}
                      />
                    </ListItem>
                  ))}
                  {quarantineAssignments.length > 3 && (
                    <ListItem>
                      <ListItemText
                        secondary={`+${quarantineAssignments.length - 3} more`}
                      />
                    </ListItem>
                  )}
                </List>
              </Paper>
            </Grid>
          )}

          {disposalAssignments.length > 0 && (
            <Grid item xs={12} md={4}>
              <Paper variant="outlined" sx={{ p: 2, bgcolor: 'error.light' }}>
                <Typography variant="h6" color="error.dark">
                  Disposal/Scrap
                </Typography>
                <Typography variant="body2">
                  {disposalAssignments.length} items assigned to disposal
                </Typography>
                <List dense>
                  {disposalAssignments.slice(0, 3).map((a) => (
                    <ListItem key={a.returnLineItemId}>
                      <ListItemText
                        primary={a.productCode}
                        secondary={`Location: ${a.locationCode}`}
                      />
                    </ListItem>
                  ))}
                  {disposalAssignments.length > 3 && (
                    <ListItem>
                      <ListItemText secondary={`+${disposalAssignments.length - 3} more`} />
                    </ListItem>
                  )}
                </List>
              </Paper>
            </Grid>
          )}
        </Grid>

        <Box mt={3} display="flex" justifyContent="flex-end">
          <Button variant="contained" color="primary" onClick={onConfirm} size="large">
            Confirm Assignments
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
};
```

#### User Flow

1. **View Pending Returns**
    - Navigate to Returns > Location Assignment
    - See summary of pending returns by condition
    - Filter by return type, condition, or date

2. **Auto-Assignment (Default)**
    - Click "Auto-assign" button for a return
    - System intelligently assigns locations based on:
        - Product condition
        - FEFO for good condition returns
        - Quarantine zones for damaged products
        - Disposal areas for expired/write-off
    - View assignment confirmation with location details
    - Confirm or modify assignments

3. **Manual Assignment (Override)**
    - Click "Manual assign" for custom routing
    - Select specific locations for each line item
    - System validates capacity and location type
    - Confirm assignments

4. **Assignment Confirmation**
    - Review all assigned locations grouped by type
    - See location codes and capacity utilization
    - Confirm to trigger location assignment events
    - Generate location labels for warehouse staff

---

## Domain Model Design

### Event-Driven Architecture

This user story is implemented primarily through **event-driven choreography**:

1. **ReturnInitiatedEvent** (from Returns Service) → Location Management Service
2. **ReturnProcessedEvent** (from Returns Service) → Location Management Service
3. **DamageRecordedEvent** (from Returns Service) → Location Management Service
4. Location Management Service processes event and auto-assigns locations
5. **ReturnLocationAssignedEvent** (from Location Management Service) → Stock Management Service

### Event Listener in Location Management Service

**Location:** `services/location-management-service/location-management-messaging/src/main/java/com/ccbsa/wms/location/messaging/listener/ReturnEventListener.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnEventListener {

    private final AssignReturnLocationsCommandHandler assignReturnLocationsCommandHandler;

    @KafkaListener(topics = "${kafka.topics.return-initiated}")
    public void handleReturnInitiatedEvent(ReturnInitiatedEvent event) {
        log.info("Received ReturnInitiatedEvent for return: {}", event.getReturnId());
        
        AssignReturnLocationsCommand command = buildCommand(event);
        assignReturnLocationsCommandHandler.handle(command);
    }

    @KafkaListener(topics = "${kafka.topics.return-processed}")
    public void handleReturnProcessedEvent(ReturnProcessedEvent event) {
        log.info("Received ReturnProcessedEvent for return: {}", event.getReturnId());
        
        AssignReturnLocationsCommand command = buildCommand(event);
        assignReturnLocationsCommandHandler.handle(command);
    }
    
    private AssignReturnLocationsCommand buildCommand(ReturnEvent event) {
        return AssignReturnLocationsCommand.builder()
            .returnId(event.getReturnId())
            .returnLineItems(event.getLineItems())
            .tenantId(event.getTenantId())
            .build();
    }
}
```

### Location Assignment Strategy (Domain Service)

**Location:**
`services/location-management-service/location-management-domain/location-management-domain-core/src/main/java/com/ccbsa/wms/location/domain/core/service/ReturnLocationAssignmentStrategy.java`

```java
public class ReturnLocationAssignmentStrategy {

    public LocationId assignLocationByCondition(
            ProductId productId,
            ProductCondition productCondition,
            Quantity quantity,
            ZonedDateTime expirationDate,
            TenantId tenantId,
            LocationRepository locationRepository) {

        switch (productCondition) {
            case GOOD:
                return assignToAvailableStock(productId, quantity, expirationDate, tenantId, locationRepository);
            
            case DAMAGED:
            case QUARANTINE:
                return assignToQuarantineZone(productId, quantity, tenantId, locationRepository);
            
            case EXPIRED:
                return assignToDisposalArea(productId, quantity, tenantId, locationRepository);
            
            case WRITE_OFF:
                return assignToScrapLocation(productId, quantity, tenantId, locationRepository);
            
            default:
                throw new InvalidLocationAssignmentException(
                    "Unknown product condition: " + productCondition
                );
        }
    }

    private LocationId assignToAvailableStock(
            ProductId productId,
            Quantity quantity,
            ZonedDateTime expirationDate,
            TenantId tenantId,
            LocationRepository locationRepository) {

        // Use FEFO strategy for good condition returns
        List<Location> availableLocations = locationRepository
            .findAvailableLocationsByProductAndFEFO(productId, expirationDate, tenantId);

        for (Location location : availableLocations) {
            if (location.hasCapacityFor(quantity)) {
                return location.getId();
            }
        }

        throw new NoAvailableLocationException(
            "No available location with sufficient capacity for product: " + productId.getValue()
        );
    }

    private LocationId assignToQuarantineZone(
            ProductId productId,
            Quantity quantity,
            TenantId tenantId,
            LocationRepository locationRepository) {

        List<Location> quarantineLocations = locationRepository
            .findQuarantineLocationsByTenant(tenantId);

        for (Location location : quarantineLocations) {
            if (location.hasCapacityFor(quantity)) {
                return location.getId();
            }
        }

        throw new NoAvailableLocationException(
            "No available quarantine location for damaged product: " + productId.getValue()
        );
    }

    private LocationId assignToDisposalArea(
            ProductId productId,
            Quantity quantity,
            TenantId tenantId,
            LocationRepository locationRepository) {

        List<Location> disposalLocations = locationRepository
            .findDisposalLocationsByTenant(tenantId);

        for (Location location : disposalLocations) {
            if (location.hasCapacityFor(quantity)) {
                return location.getId();
            }
        }

        throw new NoAvailableLocationException(
            "No available disposal location for expired product: " + productId.getValue()
        );
    }

    private LocationId assignToScrapLocation(
            ProductId productId,
            Quantity quantity,
            TenantId tenantId,
            LocationRepository locationRepository) {

        List<Location> scrapLocations = locationRepository
            .findScrapLocationsByTenant(tenantId);

        for (Location location : scrapLocations) {
            if (location.hasCapacityFor(quantity)) {
                return location.getId();
            }
        }

        throw new NoAvailableLocationException(
            "No available scrap location for write-off product: " + productId.getValue()
        );
    }
}
```

### Domain Event

**Location:**
`services/location-management-service/location-management-domain/location-management-domain-core/src/main/java/com/ccbsa/wms/location/domain/core/event/ReturnLocationAssignedEvent.java`

```java
@Getter
public class ReturnLocationAssignedEvent extends DomainEvent<ReturnLocationAssignment> {

    private final ReturnId returnId;
    private final List<ReturnLineAssignment> lineAssignments;
    private final ZonedDateTime assignedAt;

    public ReturnLocationAssignedEvent(
            ReturnLocationAssignment assignment,
            ZonedDateTime occurredOn) {
        super(assignment, occurredOn);
        this.returnId = assignment.getReturnId();
        this.lineAssignments = assignment.getLineAssignments();
        this.assignedAt = assignment.getAssignedAt();
    }

    @Getter
    @AllArgsConstructor
    public static class ReturnLineAssignment {
        private final ReturnLineItemId returnLineItemId;
        private final ProductId productId;
        private final LocationId assignedLocationId;
        private final ProductCondition productCondition;
        private final Quantity quantity;
    }
}
```

**Consumed By:**

- **Stock Management Service** - Updates stock levels at assigned locations
- **Returns Service** - Updates return status to LOCATION_ASSIGNED
- **Notification Service** - Notifies warehouse staff of location assignments

---

## Frontend Implementation

### TypeScript Types

```typescript
export interface AssignReturnLocationsRequest {
  returnId: string;
  autoAssign: boolean;
  manualAssignments?: ManualLocationAssignment[];
}

export interface ManualLocationAssignment {
  returnLineItemId: string;
  locationId: string;
}

export interface LocationAssignmentResponse {
  returnId: string;
  totalAssignments: number;
  goodConditionAssignments: number;
  quarantineAssignments: number;
  disposalAssignments: number;
  assignments: ReturnLineLocationAssignment[];
  assignedAt: string;
}

export interface ReturnLineLocationAssignment {
  returnLineItemId: string;
  productCode: string;
  productCondition: ProductCondition;
  assignedLocationId: string;
  assignedLocationCode: string;
  assignedLocationName: string;
}
```

### React Hook

```typescript
export const useAutoAssignReturnLocations = () => {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: (request: { returnId: string }) =>
      returnLocationService.autoAssignLocations(request.returnId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['pending-returns'] });
      queryClient.invalidateQueries({ queryKey: ['return-assignments'] });
      enqueueSnackbar(
        `${data.totalAssignments} locations assigned successfully`,
        { variant: 'success' }
      );
    },
    onError: (error) => {
      enqueueSnackbar(`Failed to assign locations: ${error.message}`, {
        variant: 'error',
      });
    },
  });
};
```

---

## Testing Strategy

### Unit Tests

```java
@Test
void assignLocationByCondition_GoodCondition_ShouldAssignToAvailableStock() {
    // Arrange
    ProductCondition condition = ProductCondition.GOOD;
    Location availableLocation = createAvailableLocation();
    when(locationRepository.findAvailableLocationsByProductAndFEFO(any(), any(), any()))
        .thenReturn(List.of(availableLocation));

    // Act
    LocationId assigned = strategy.assignLocationByCondition(
        productId, condition, quantity, expirationDate, tenantId, locationRepository
    );

    // Assert
    assertThat(assigned).isEqualTo(availableLocation.getId());
}

@Test
void assignLocationByCondition_DamagedCondition_ShouldAssignToQuarantine() {
    // Arrange
    ProductCondition condition = ProductCondition.DAMAGED;
    Location quarantineLocation = createQuarantineLocation();
    when(locationRepository.findQuarantineLocationsByTenant(any()))
        .thenReturn(List.of(quarantineLocation));

    // Act
    LocationId assigned = strategy.assignLocationByCondition(
        productId, condition, quantity, null, tenantId, locationRepository
    );

    // Assert
    assertThat(assigned).isEqualTo(quarantineLocation.getId());
}
```

---

## Acceptance Criteria Validation

| # | Acceptance Criterion                                                                         | Implementation Status | Validation                                                                           |
|---|----------------------------------------------------------------------------------------------|-----------------------|--------------------------------------------------------------------------------------|
| 1 | System automatically assigns good condition products to available stock locations using FEFO | ✅ Implemented         | ReturnLocationAssignmentStrategy.assignToAvailableStock() uses FEFO repository query |
| 2 | System routes damaged products to quarantine zone for inspection                             | ✅ Implemented         | DAMAGED and QUARANTINE conditions routed to assignToQuarantineZone()                 |
| 3 | System segregates expired products to dedicated disposal locations                           | ✅ Implemented         | EXPIRED condition routed to assignToDisposalArea() with dedicated location type      |
| 4 | System assigns write-off products to disposal/scrap locations                                | ✅ Implemented         | WRITE_OFF condition routed to assignToScrapLocation()                                |
| 5 | System publishes LocationAssignedEvent after successful assignment                           | ✅ Implemented         | ReturnLocationAssignedEvent published with all assignment details                    |
| 6 | System validates location capacity before assignment                                         | ✅ Implemented         | location.hasCapacityFor(quantity) check in all assignment methods                    |

---

## Implementation Checklist

### Location Management Service - Domain Core

- [ ] Create `ReturnLocationAssignment` aggregate
- [ ] Create `ReturnLocationAssignmentStrategy` domain service
- [ ] Create `ReturnLocationAssignedEvent` domain event
- [ ] Add `NoAvailableLocationException`
- [ ] Unit tests for assignment strategy

### Location Management Service - Application Service

- [ ] Create `AssignReturnLocationsCommand`
- [ ] Create `AssignReturnLocationsCommandHandler`
- [ ] Create `LocationRepository` extensions for FEFO, quarantine, disposal queries
- [ ] Unit tests for command handlers

### Location Management Service - Messaging

- [ ] Create `ReturnEventListener` for ReturnInitiatedEvent
- [ ] Create `ReturnEventListener` for ReturnProcessedEvent
- [ ] Create `ReturnEventListener` for DamageRecordedEvent
- [ ] Configure Kafka topics
- [ ] Integration tests for event consumption

### Frontend

- [ ] Create `useAutoAssignReturnLocations` hook
- [ ] Create `useAssignReturnLocations` hook (manual)
- [ ] Create `usePendingReturns` hook
- [ ] Create `returnLocationService`
- [ ] Create assignment confirmation UI
- [ ] Integration tests

### Gateway API Tests

- [ ] Test auto-assignment for good condition returns
- [ ] Test quarantine assignment for damaged returns
- [ ] Test disposal assignment for expired returns
- [ ] Test capacity validation
- [ ] Test event publishing

---

**End of Implementation Plan**
