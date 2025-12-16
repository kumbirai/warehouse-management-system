# Location Management Implementation Plan

## US-3.1.1: Create Warehouse Location with Barcode

**Service:** Location Management Service  
**Priority:** Must Have  
**Story Points:** 5  
**Sprint:** Sprint 1

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

**As a** warehouse administrator  
**I want** to create warehouse locations with barcodes  
**So that** all locations can be scanned and tracked

### Business Requirements

- Create warehouse locations with unique barcode identifiers
- Barcode format follows CCBSA standards
- Validate barcode uniqueness
- Support barcode scanning for location identification
- Location barcodes are printable and replaceable
- Store location coordinates (zone, aisle, rack, level)

### Technical Requirements

- Follow DDD, Clean Hexagonal Architecture, CQRS, Event-Driven Choreography
- Multi-tenant support (tenant isolation)
- Event publishing for location creation
- REST API with proper error handling
- Frontend form with validation

---

## UI Design

### Location Creation Form

**Component:** `LocationCreationForm.tsx`

**Fields:**

- **Zone** (required, text input)
- **Aisle** (required, text input)
- **Rack** (required, text input)
- **Level** (required, text input)
- **Barcode** (optional, text input - auto-generated if not provided)
- **Description** (optional, textarea)
- **Status** (default: AVAILABLE, dropdown)

**Validation:**

- Zone, Aisle, Rack, Level are required
- Barcode format validation (if manually entered)
- Real-time validation feedback

**Actions:**

- **Create** - Submit form to create location
- **Cancel** - Navigate back to location list
- **Generate Barcode** - Auto-generate barcode based on coordinates

**UI Flow:**

1. User navigates to "Locations" → "Create Location"
2. Form displays with all fields
3. User enters location coordinates
4. User optionally enters barcode or clicks "Generate Barcode"
5. User clicks "Create"
6. System validates and creates location
7. Success message displayed
8. User redirected to location detail page

### Location List View

**Component:** `LocationList.tsx`

**Features:**

- List all locations with pagination
- Filter by zone, aisle, status
- Search by barcode or description
- Sort by zone, aisle, rack, level
- Display barcode with QR code option
- Actions: View, Edit, Delete

### Location Detail View

**Component:** `LocationDetail.tsx`

**Features:**

- Display location details
- Display barcode (with QR code)
- Print barcode option
- Location status management
- Edit location option

---

## Domain Model Design

### Location Aggregate Root

**Package:** `com.ccbsa.wms.location.domain.core.entity`

```java
public class Location extends TenantAwareAggregateRoot<LocationId> {
    
    private LocationBarcode barcode;
    private LocationCoordinates coordinates;
    private LocationStatus status;
    private LocationCapacity capacity;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    
    // Business logic methods
    public static Builder builder() { ... }
    
    public void updateBarcode(LocationBarcode newBarcode) { ... }
    
    public void updateStatus(LocationStatus newStatus) { ... }
    
    public void updateCapacity(LocationCapacity newCapacity) { ... }
    
    public boolean isAvailable() { ... }
    
    public boolean canAccommodate(Quantity quantity) { ... }
}
```

### Value Objects

**LocationId:**

```java
public final class LocationId {
    private final UUID value;
    
    public static LocationId of(UUID value) { ... }
    public static LocationId generate() { ... }
}
```

**LocationBarcode:**

```java
public final class LocationBarcode {
    private final String value;
    
    public static LocationBarcode of(String value) { ... }
    public static LocationBarcode generate(LocationCoordinates coordinates) { ... }
    
    private void validateFormat() { ... } // CCBSA standards
}
```

**LocationCoordinates:**

```java
public final class LocationCoordinates {
    private final String zone;
    private final String aisle;
    private final String rack;
    private final String level;
    
    public static Builder builder() { ... }
}
```

**LocationStatus:**

```java
public enum LocationStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    BLOCKED
}
```

**LocationCapacity:**

```java
public final class LocationCapacity {
    private final Quantity currentQuantity;
    private final Quantity maximumQuantity;
    
    public boolean canAccommodate(Quantity additionalQuantity) { ... }
}
```

### Domain Events

**LocationCreatedEvent:**

```java
public class LocationCreatedEvent extends LocationManagementEvent<Location> {
    private final LocationBarcode barcode;
    private final LocationCoordinates coordinates;
    private final LocationStatus status;
    
    // Constructor and getters
}
```

---

## Backend Implementation

### Phase 1: Domain Core

**Module:** `location-management-domain/location-management-domain-core`

**Files to Create:**

1. `Location.java` - Aggregate root
2. `LocationId.java` - Value object
3. `LocationBarcode.java` - Value object
4. `LocationCoordinates.java` - Value object
5. `LocationStatus.java` - Enum
6. `LocationCapacity.java` - Value object
7. `LocationCreatedEvent.java` - Domain event
8. `LocationManagementEvent.java` - Base service event

**Implementation Steps:**

1. Create value objects with validation
2. Create Location aggregate with builder pattern
3. Implement business logic methods
4. Create domain events
5. Write unit tests for domain logic

### Phase 2: Application Service

**Module:** `location-management-domain/location-management-application-service`

**Command Handler:**

```java
@Component
public class CreateLocationCommandHandler {
    
    private final LocationRepository repository;
    private final LocationEventPublisher eventPublisher;
    
    @Transactional
    public CreateLocationResult handle(CreateLocationCommand command) {
        // 1. Validate barcode uniqueness
        if (command.getBarcode() != null) {
            validateBarcodeUniqueness(command.getBarcode(), command.getTenantId());
        }
        
        // 2. Generate barcode if not provided
        LocationBarcode barcode = command.getBarcode() != null 
            ? command.getBarcode() 
            : LocationBarcode.generate(command.getCoordinates());
        
        // 3. Create location aggregate
        Location location = Location.builder()
            .locationId(LocationId.generate())
            .tenantId(command.getTenantId())
            .barcode(barcode)
            .coordinates(command.getCoordinates())
            .status(LocationStatus.AVAILABLE)
            .capacity(LocationCapacity.empty())
            .description(command.getDescription())
            .build();
        
        // 4. Persist
        repository.save(location);
        
        // 5. Publish events
        eventPublisher.publish(location.getDomainEvents());
        location.clearDomainEvents();
        
        // 6. Return result
        return CreateLocationResult.builder()
            .locationId(location.getId())
            .barcode(location.getBarcode())
            .coordinates(location.getCoordinates())
            .status(location.getStatus())
            .build();
    }
    
    private void validateBarcodeUniqueness(LocationBarcode barcode, TenantId tenantId) {
        if (repository.existsByBarcodeAndTenantId(barcode, tenantId)) {
            throw new BarcodeAlreadyExistsException(barcode);
        }
    }
}
```

**Query Handler:**

```java
@Component
public class GetLocationQueryHandler {
    
    private final LocationRepository repository;
    
    @Transactional(readOnly = true)
    public LocationQueryResult handle(GetLocationQuery query) {
        Location location = repository.findByIdAndTenantId(
            query.getLocationId(), 
            query.getTenantId()
        ).orElseThrow(() -> new LocationNotFoundException(query.getLocationId()));
        
        return LocationQueryResult.builder()
            .locationId(location.getId())
            .barcode(location.getBarcode())
            .coordinates(location.getCoordinates())
            .status(location.getStatus())
            .capacity(location.getCapacity())
            .description(location.getDescription())
            .createdAt(location.getCreatedAt())
            .build();
    }
}
```

**Port Interfaces:**

```java
// Repository Port
public interface LocationRepository {
    void save(Location location);
    Optional<Location> findByIdAndTenantId(LocationId id, TenantId tenantId);
    boolean existsByBarcodeAndTenantId(LocationBarcode barcode, TenantId tenantId);
    List<Location> findByTenantId(TenantId tenantId);
}

// Event Publisher Port
public interface LocationEventPublisher extends EventPublisher {
    void publish(LocationManagementEvent<?> event);
}
```

### Phase 3: Data Access

**Module:** `location-management-dataaccess`

**JPA Entity:**

```java
@Entity
@Table(name = "locations", schema = "tenant_schema")
public class LocationEntity {
    @Id
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "barcode", nullable = false, unique = true)
    private String barcode;
    
    @Column(name = "zone", nullable = false)
    private String zone;
    
    @Column(name = "aisle", nullable = false)
    private String aisle;
    
    @Column(name = "rack", nullable = false)
    private String rack;
    
    @Column(name = "level", nullable = false)
    private String level;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LocationStatus status;
    
    @Column(name = "current_quantity")
    private BigDecimal currentQuantity;
    
    @Column(name = "maximum_quantity")
    private BigDecimal maximumQuantity;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;
    
    @Version
    private Long version;
    
    // Getters and setters
}
```

**Repository Adapter:**

```java
@Repository
public class LocationRepositoryAdapter implements LocationRepository {
    
    private final LocationJpaRepository jpaRepository;
    private final LocationEntityMapper mapper;
    
    @Override
    public void save(Location location) {
        Optional<LocationEntity> existing = jpaRepository.findByTenantIdAndId(
            location.getTenantId().getValue(),
            location.getId().getValue()
        );
        
        LocationEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            updateEntityFromDomain(entity, location);
        } else {
            entity = mapper.toEntity(location);
        }
        
        jpaRepository.save(entity);
    }
    
    @Override
    public Optional<Location> findByIdAndTenantId(LocationId id, TenantId tenantId) {
        return jpaRepository.findByTenantIdAndId(tenantId.getValue(), id.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    public boolean existsByBarcodeAndTenantId(LocationBarcode barcode, TenantId tenantId) {
        return jpaRepository.existsByTenantIdAndBarcode(
            tenantId.getValue(), 
            barcode.getValue()
        );
    }
}
```

**Database Migration:**

```sql
-- V1__Create_locations_table.sql
CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    barcode VARCHAR(255) NOT NULL,
    zone VARCHAR(100) NOT NULL,
    aisle VARCHAR(100) NOT NULL,
    rack VARCHAR(100) NOT NULL,
    level VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    current_quantity DECIMAL(18, 2) DEFAULT 0,
    maximum_quantity DECIMAL(18, 2),
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_locations_tenant_barcode UNIQUE (tenant_id, barcode)
);

CREATE INDEX idx_locations_tenant_id ON locations(tenant_id);
CREATE INDEX idx_locations_barcode ON locations(barcode);
CREATE INDEX idx_locations_coordinates ON locations(zone, aisle, rack, level);
```

### Phase 4: REST API

**Module:** `location-management-application`

**Command Controller:**

```java
@RestController
@RequestMapping("/api/v1/location-management/locations")
@Tag(name = "Location Commands", description = "Location command operations")
public class LocationCommandController {
    
    private final CreateLocationCommandHandler commandHandler;
    private final LocationDTOMapper mapper;
    
    @PostMapping
    @Operation(summary = "Create a new location")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<CreateLocationResultDTO>> createLocation(
            @Valid @RequestBody CreateLocationCommandDTO commandDTO,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        CreateLocationCommand command = mapper.toCommand(commandDTO, TenantId.of(tenantId));
        CreateLocationResult result = commandHandler.handle(command);
        CreateLocationResultDTO resultDTO = mapper.toDTO(result);
        
        return ApiResponseBuilder.created(resultDTO);
    }
}
```

**Query Controller:**

```java
@RestController
@RequestMapping("/api/v1/location-management/locations")
@Tag(name = "Location Queries", description = "Location query operations")
public class LocationQueryController {
    
    private final GetLocationQueryHandler queryHandler;
    private final LocationDTOMapper mapper;
    
    @GetMapping("/{locationId}")
    @Operation(summary = "Get location by ID")
    @PreAuthorize("hasRole('VIEWER')")
    public ResponseEntity<ApiResponse<LocationQueryResultDTO>> getLocation(
            @PathVariable String locationId,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        GetLocationQuery query = GetLocationQuery.builder()
            .locationId(LocationId.of(UUID.fromString(locationId)))
            .tenantId(TenantId.of(tenantId))
            .build();
        
        LocationQueryResult result = queryHandler.handle(query);
        LocationQueryResultDTO resultDTO = mapper.toDTO(result);
        
        return ApiResponseBuilder.ok(resultDTO);
    }
}
```

**DTOs:**

```java
// CreateLocationCommandDTO
public class CreateLocationCommandDTO {
    @NotBlank
    private String zone;
    
    @NotBlank
    private String aisle;
    
    @NotBlank
    private String rack;
    
    @NotBlank
    private String level;
    
    private String barcode; // Optional, auto-generated if not provided
    
    private String description;
    
    // Getters and setters
}

// CreateLocationResultDTO
public class CreateLocationResultDTO {
    private String locationId;
    private String barcode;
    private LocationCoordinatesDTO coordinates;
    private String status;
    
    // Getters and setters
}
```

### Phase 5: Messaging

**Module:** `location-management-messaging`

**Event Publisher:**

```java
@Component
public class LocationEventPublisherImpl implements LocationEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper kafkaObjectMapper;
    
    @Override
    public void publish(List<DomainEvent<?>> events) {
        events.forEach(event -> {
            String topic = "location-management-events";
            String key = event.getAggregateId().toString();
            
            try {
                String json = kafkaObjectMapper.writeValueAsString(event);
                kafkaTemplate.send(topic, key, json);
            } catch (Exception e) {
                throw new EventPublishingException("Failed to publish event", e);
            }
        });
    }
}
```

---

## Frontend Implementation

### API Client

**File:** `frontend-app/src/services/locationApiClient.ts`

```typescript
export interface CreateLocationCommandDTO {
  zone: string;
  aisle: string;
  rack: string;
  level: string;
  barcode?: string;
  description?: string;
}

export interface LocationQueryResultDTO {
  locationId: string;
  barcode: string;
  coordinates: {
    zone: string;
    aisle: string;
    rack: string;
    level: string;
  };
  status: string;
  capacity: {
    currentQuantity: number;
    maximumQuantity: number;
  };
  description?: string;
  createdAt: string;
}

class LocationApiClient {
  async createLocation(command: CreateLocationCommandDTO): Promise<LocationQueryResultDTO> {
    const response = await apiClient.post<ApiResponse<LocationQueryResultDTO>>(
      '/location-management/locations',
      command
    );
    return response.data.data;
  }
  
  async getLocation(locationId: string): Promise<LocationQueryResultDTO> {
    const response = await apiClient.get<ApiResponse<LocationQueryResultDTO>>(
      `/location-management/locations/${locationId}`
    );
    return response.data.data;
  }
  
  async listLocations(params: {
    page?: number;
    size?: number;
    zone?: string;
    status?: string;
  }): Promise<Page<LocationQueryResultDTO>> {
    const response = await apiClient.get<ApiResponse<Page<LocationQueryResultDTO>>>(
      '/location-management/locations',
      { params }
    );
    return response.data.data;
  }
}

export const locationApiClient = new LocationApiClient();
```

### React Components

**Location Creation Form:**

```typescript
// frontend-app/src/features/location/commands/CreateLocationForm.tsx
export const CreateLocationForm: React.FC = () => {
  const { register, handleSubmit, formState: { errors } } = useForm<CreateLocationCommandDTO>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();
  
  const onSubmit = async (data: CreateLocationCommandDTO) => {
    setIsSubmitting(true);
    try {
      const location = await locationApiClient.createLocation(data);
      toast.success('Location created successfully');
      navigate(`/locations/${location.locationId}`);
    } catch (error) {
      toast.error('Failed to create location');
    } finally {
      setIsSubmitting(false);
    }
  };
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <TextField
        {...register('zone', { required: 'Zone is required' })}
        label="Zone"
        error={!!errors.zone}
        helperText={errors.zone?.message}
      />
      {/* Other fields */}
      <Button type="submit" disabled={isSubmitting}>
        Create Location
      </Button>
    </form>
  );
};
```

---

## Data Flow

### Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React)                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  LocationCreationForm                                │   │
│  │  - User enters zone, aisle, rack, level              │   │
│  │  - Optionally enters barcode                          │   │
│  │  - Clicks "Create"                                   │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │ POST /api/v1/location-management/locations
│                      │ { zone, aisle, rack, level, barcode? }
└──────────────────────┼───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│                  Gateway Service                              │
│  - Validates authentication                                   │
│  - Extracts tenant ID from token                             │
│  - Routes to Location Management Service                     │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│         Location Management Service                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  LocationCommandController                            │   │
│  │  - Validates DTO                                      │   │
│  │  - Maps to CreateLocationCommand                      │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │
│  ┌───────────────────▼──────────────────────────────────┐   │
│  │  CreateLocationCommandHandler                         │   │
│  │  - Validates barcode uniqueness                       │   │
│  │  - Generates barcode if not provided                  │   │
│  │  - Creates Location aggregate                         │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │
│  ┌───────────────────▼──────────────────────────────────┐   │
│  │  LocationRepositoryAdapter                           │   │
│  │  - Saves LocationEntity to database                   │   │
│  └───────────────────┬──────────────────────────────────┘   │
│                      │
│  ┌───────────────────▼──────────────────────────────────┐   │
│  │  LocationEventPublisher                              │   │
│  │  - Publishes LocationCreatedEvent to Kafka           │   │
│  └───────────────────┬──────────────────────────────────┘   │
└──────────────────────┼───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│                    Kafka                                       │
│  Topic: location-management-events                            │
│  Event: LocationCreatedEvent                                   │
└───────────────────────────────────────────────────────────────┘
```

---

## Testing Strategy

### Unit Tests

**Domain Core:**

- Location aggregate creation
- Barcode generation logic
- Barcode format validation
- Business logic methods

**Application Service:**

- Command handler logic
- Query handler logic
- Validation logic

**Data Access:**

- Repository adapter operations
- Entity mapping

### Integration Tests

- End-to-end location creation
- Database operations
- Event publishing

### Gateway API Tests

**File:** `gateway-api-tests/src/test/java/com/ccbsa/wms/gateway/api/LocationManagementTest.java`

```java
@DisplayName("Location Management API Tests")
class LocationManagementTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Should create location with valid data")
    void shouldCreateLocation() {
        Map<String, Object> createLocationRequest = new HashMap<>();
        createLocationRequest.put("zone", "A");
        createLocationRequest.put("aisle", "01");
        createLocationRequest.put("rack", "01");
        createLocationRequest.put("level", "01");
        createLocationRequest.put("description", "Main storage area");
        
        RequestHeaderHelper.addTenantHeaderIfNeeded(
            webTestClient
                .post()
                .uri("/location-management/locations")
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(createLocationRequest)),
            authHelper,
            accessToken)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.locationId").exists()
            .jsonPath("$.data.barcode").exists();
    }
}
```

---

## Acceptance Criteria Validation

| AC  | Description                                                       | Validation Method                              |
|-----|-------------------------------------------------------------------|------------------------------------------------|
| AC1 | System allows creation of location with unique barcode identifier | Unit test + Integration test                   |
| AC2 | Barcode format follows CCBSA standards                            | Unit test for barcode validation               |
| AC3 | System validates barcode uniqueness                               | Integration test + Unit test                   |
| AC4 | System supports barcode scanning for location identification      | Frontend barcode scanner component             |
| AC5 | Location barcodes are printable and replaceable                   | Frontend print functionality + Update endpoint |
| AC6 | System stores location coordinates (zone, aisle, rack, level)     | Database migration + Integration test          |

---

## Definition of Done

- [ ] Domain core implemented with all value objects and aggregate
- [ ] Application service implemented with command/query handlers
- [ ] Data access implemented with JPA entities and repository adapters
- [ ] REST API implemented with proper DTOs and error handling
- [ ] Event publishing implemented
- [ ] Frontend form implemented with validation
- [ ] Gateway API tests written and passing
- [ ] Unit tests written (80%+ coverage)
- [ ] Integration tests written and passing
- [ ] Code reviewed and approved
- [ ] Documentation updated

---

**Document Control**

- **Version:** 1.0
- **Date:** 2025-01
- **Status:** Draft

