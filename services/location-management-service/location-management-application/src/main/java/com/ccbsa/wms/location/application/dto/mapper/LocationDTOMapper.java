package com.ccbsa.wms.location.application.dto.mapper;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.ExpirationDate;
import com.ccbsa.common.domain.valueobject.StockClassification;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.dto.command.CreateLocationCommandDTO;
import com.ccbsa.wms.location.application.dto.command.CreateLocationResultDTO;
import com.ccbsa.wms.location.application.dto.command.LocationCoordinatesDTO;
import com.ccbsa.wms.location.application.dto.command.UpdateLocationCommandDTO;
import com.ccbsa.wms.location.application.dto.command.UpdateLocationStatusCommandDTO;
import com.ccbsa.wms.location.application.dto.command.UpdateLocationStatusResultDTO;
import com.ccbsa.wms.location.application.dto.query.ListLocationsQueryResultDTO;
import com.ccbsa.wms.location.application.dto.query.LocationAvailabilityQueryResultDTO;
import com.ccbsa.wms.location.application.dto.query.LocationCapacityDTO;
import com.ccbsa.wms.location.application.dto.query.LocationQueryResultDTO;
import com.ccbsa.wms.location.application.service.command.dto.AssignLocationsFEFOCommand;
import com.ccbsa.wms.location.application.service.command.dto.CreateLocationCommand;
import com.ccbsa.wms.location.application.service.command.dto.CreateLocationResult;
import com.ccbsa.wms.location.application.service.command.dto.UpdateLocationCommand;
import com.ccbsa.wms.location.application.service.command.dto.UpdateLocationStatusCommand;
import com.ccbsa.wms.location.application.service.command.dto.UpdateLocationStatusResult;
import com.ccbsa.wms.location.application.service.query.dto.CheckLocationAvailabilityQuery;
import com.ccbsa.wms.location.application.service.query.dto.GetLocationQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListLocationsQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListLocationsQueryResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationAvailabilityResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCapacity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.location.domain.core.valueobject.LocationStatus;
import com.ccbsa.wms.location.domain.core.valueobject.StockItemAssignmentRequest;

/**
 * DTO Mapper: LocationDTOMapper
 * <p>
 * Maps between API DTOs and application service commands/queries. Acts as an anti-corruption layer protecting the domain from external API changes.
 */
@Component
public class LocationDTOMapper {

    /**
     * Converts CreateLocationCommandDTO to CreateLocationCommand.
     *
     * @param dto      Command DTO
     * @param tenantId Tenant identifier string
     * @return CreateLocationCommand
     */
    public CreateLocationCommand toCreateCommand(CreateLocationCommandDTO dto, String tenantId) {
        CreateLocationCommand.Builder builder = CreateLocationCommand.builder().tenantId(TenantId.of(tenantId));

        // Determine coordinates based on model type
        // Priority: coordinate-based model > hierarchical model > defaults
        LocationCoordinates coordinates;
        if (dto.isCoordinateBasedModel() && (dto.getZone() != null || dto.getAisle() != null || dto.getRack() != null || dto.getLevel() != null)) {
            // Coordinate-based model: use provided coordinates (fill with defaults if partially provided)
            coordinates = LocationCoordinates.of(dto.getZone() != null && !dto.getZone().trim().isEmpty() ? dto.getZone() : "00",
                    dto.getAisle() != null && !dto.getAisle().trim().isEmpty() ? dto.getAisle() : "00",
                    dto.getRack() != null && !dto.getRack().trim().isEmpty() ? dto.getRack() : "00",
                    dto.getLevel() != null && !dto.getLevel().trim().isEmpty() ? dto.getLevel() : "00");
        } else if (dto.isHierarchicalModel()) {
            // Hierarchical model: generate coordinates from location type and code
            coordinates = generateCoordinatesFromHierarchy(dto);
        } else {
            // Fallback: generate default coordinates
            coordinates = generateDefaultCoordinates(dto);
        }

        builder.coordinates(coordinates);

        // Set barcode if provided
        if (dto.getBarcode() != null && !dto.getBarcode().trim().isEmpty()) {
            builder.barcode(LocationBarcode.of(dto.getBarcode()));
        }

        // Set description - prefer name from hierarchical model, fallback to description
        String description = dto.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = dto.getName();
        }
        if (description != null && !description.trim().isEmpty()) {
            builder.description(description);
        }

        // Pass through code/name/type/parentLocationId for response mapping
        builder.code(dto.getCode());
        builder.name(dto.getName());
        builder.type(dto.getType());
        builder.parentLocationId(dto.getParentLocationId());

        return builder.build();
    }

    /**
     * Generates coordinates from hierarchical location model.
     * For warehouses, uses code as zone and generates defaults for other fields.
     * For other types, derives coordinates from hierarchy or uses defaults.
     * <p>
     * Note: All coordinate values (zone, aisle, rack, level) must not exceed 10 characters
     * as per LocationCoordinates validation rules.
     *
     * @param dto Command DTO
     * @return LocationCoordinates
     */
    private LocationCoordinates generateCoordinatesFromHierarchy(CreateLocationCommandDTO dto) {
        String type = dto.getType() != null ? dto.getType().toUpperCase(Locale.ROOT) : "";
        String code = dto.getCode() != null ? dto.getCode() : "";

        // Sanitize and truncate code to ensure it fits within 10-character limit for coordinates
        // This ensures barcode generation produces valid alphanumeric strings and coordinates pass validation
        String sanitizedCode = sanitizeAndTruncateForCoordinate(code, 10);

        switch (type) {
            case "WAREHOUSE":
                // For warehouses, use sanitized code as zone (max 10 chars), generate defaults for others
                return LocationCoordinates.of(sanitizedCode.isEmpty() ? "WH" : sanitizedCode, "00", "00", "00");
            case "ZONE":
                // For zones, use sanitized code as zone identifier (max 10 chars)
                return LocationCoordinates.of(sanitizedCode.isEmpty() ? "ZONE" : sanitizedCode, "00", "00", "00");
            case "AISLE":
                // For aisles, derive from parent or use sanitized code (max 10 chars)
                return LocationCoordinates.of("ZONE", // Will be derived from parent in future
                        sanitizedCode.isEmpty() ? "AISLE" : sanitizedCode, "00", "00");
            case "RACK":
                // For racks, derive from parent hierarchy, use sanitized code (max 10 chars)
                return LocationCoordinates.of("ZONE", // Will be derived from parent in future
                        "AISLE", // Will be derived from parent in future
                        sanitizedCode.isEmpty() ? "RACK" : sanitizedCode, "00");
            case "BIN":
            case "LEVEL":
                // For bins/levels, derive from parent hierarchy, use sanitized code (max 10 chars)
                return LocationCoordinates.of("ZONE", // Will be derived from parent in future
                        "AISLE", // Will be derived from parent in future
                        "RACK", // Will be derived from parent in future
                        sanitizedCode.isEmpty() ? "BIN" : sanitizedCode);
            default:
                // Default: use sanitized code as zone if available (max 10 chars)
                return LocationCoordinates.of(sanitizedCode.isEmpty() ? "LOC" : sanitizedCode, "00", "00", "00");
        }
    }

    /**
     * Generates default coordinates when no model is specified.
     * <p>
     * Note: Zone value must not exceed 10 characters as per LocationCoordinates validation rules.
     *
     * @param dto Command DTO
     * @return LocationCoordinates with default values
     */
    private LocationCoordinates generateDefaultCoordinates(CreateLocationCommandDTO dto) {
        // Use sanitized and truncated code if available, otherwise generate defaults
        String code = dto.getCode() != null ? dto.getCode() : "";
        String zone = sanitizeAndTruncateForCoordinate(code, 10);
        if (zone.isEmpty()) {
            zone = "DEFAULT";
        }
        return LocationCoordinates.of(zone, "00", "00", "00");
    }

    /**
     * Sanitizes and truncates a string for use as a coordinate value (zone, aisle, rack, level).
     * Coordinate values must not exceed 10 characters as per LocationCoordinates validation rules.
     * <p>
     * This method:
     * 1. Removes all non-alphanumeric characters
     * 2. Converts to uppercase
     * 3. Truncates to the specified maximum length
     *
     * @param value     String value to sanitize and truncate
     * @param maxLength Maximum length (typically 10 for coordinates)
     * @return Sanitized and truncated string with only alphanumeric characters, uppercase, max length
     */
    private String sanitizeAndTruncateForCoordinate(String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        // Remove all non-alphanumeric characters and convert to uppercase
        String sanitized = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        // Truncate to max length if necessary
        if (sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }
        return sanitized;
    }

    /**
     * Converts CreateLocationResult to CreateLocationResultDTO.
     *
     * @param result Command result
     * @return CreateLocationResultDTO
     */
    public CreateLocationResultDTO toCreateResultDTO(CreateLocationResult result) {
        CreateLocationResultDTO dto = new CreateLocationResultDTO();
        dto.setLocationId(result.getLocationId().getValueAsString());
        dto.setBarcode(result.getBarcode().getValue());
        dto.setCoordinates(toCommandCoordinatesDTO(result.getCoordinates()));
        dto.setStatus(result.getStatus().name());
        dto.setCreatedAt(result.getCreatedAt());

        // Set code/name/type/path from result (path is already generated hierarchically in command handler)
        // Note: result.getCode(), getName(), getType() already return String values from LocationQueryResult
        dto.setCode(result.getCode());
        dto.setName(result.getName());
        dto.setType(result.getType());
        dto.setPath(result.getPath());

        return dto;
    }

    /**
     * Converts LocationCoordinates to command LocationCoordinatesDTO.
     */
    private LocationCoordinatesDTO toCommandCoordinatesDTO(LocationCoordinates coordinates) {
        return new LocationCoordinatesDTO(coordinates.getZone(), coordinates.getAisle(), coordinates.getRack(), coordinates.getLevel());
    }

    /**
     * Converts location ID string and tenant ID to GetLocationQuery.
     *
     * @param locationId Location ID string
     * @param tenantId   Tenant ID string
     * @return GetLocationQuery
     */
    public GetLocationQuery toGetLocationQuery(String locationId, String tenantId) {
        return GetLocationQuery.builder().locationId(LocationId.of(UUID.fromString(locationId))).tenantId(TenantId.of(tenantId)).build();
    }

    /**
     * Converts request parameters to ListLocationsQuery.
     *
     * @param tenantId Tenant ID string
     * @param page     Page number (optional)
     * @param size     Page size (optional)
     * @param zone     Zone filter (optional)
     * @param status   Status filter (optional)
     * @param search   Search term (optional)
     * @return ListLocationsQuery
     */
    public ListLocationsQuery toListLocationsQuery(String tenantId, Integer page, Integer size, String zone, String status, String search) {
        ListLocationsQuery.Builder builder = ListLocationsQuery.builder().tenantId(TenantId.of(tenantId));

        if (page != null) {
            builder.page(page);
        }
        if (size != null) {
            builder.size(size);
        }
        if (zone != null && !zone.trim().isEmpty()) {
            builder.zone(zone.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            builder.status(status.trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            builder.search(search.trim());
        }

        return builder.build();
    }

    /**
     * Converts ListLocationsQueryResult to ListLocationsQueryResultDTO.
     *
     * @param result Query result
     * @return ListLocationsQueryResultDTO
     */
    public ListLocationsQueryResultDTO toListLocationsQueryResultDTO(ListLocationsQueryResult result) {
        ListLocationsQueryResultDTO dto = new ListLocationsQueryResultDTO();

        // Map locations
        List<LocationQueryResultDTO> locationDTOs = result.getLocations().stream().map(this::toQueryResultDTO).collect(Collectors.toList());
        dto.setLocations(locationDTOs);

        dto.setTotalCount(result.getTotalCount());
        dto.setPage(result.getPage());
        dto.setSize(result.getSize());

        return dto;
    }

    /**
     * Converts LocationQueryResult to LocationQueryResultDTO.
     *
     * @param result Query result
     * @return LocationQueryResultDTO
     */
    public LocationQueryResultDTO toQueryResultDTO(LocationQueryResult result) {
        LocationQueryResultDTO dto = new LocationQueryResultDTO();
        dto.setLocationId(result.getLocationId().getValueAsString());
        dto.setBarcode(result.getBarcode().getValue());
        dto.setCoordinates(toQueryCoordinatesDTO(result.getCoordinates()));
        dto.setStatus(result.getStatus().name());

        // Set capacity DTO - getCapacity() will extract Integer value for JSON
        LocationCapacityDTO capacityDTO = toCapacityDTO(result.getCapacity());
        dto.setCapacityDTO(capacityDTO);

        // Set code, name, type, path from query result (now stored in domain)
        // Note: result.getCode(), getName(), getType(), getDescription() already return String values from LocationQueryResult
        dto.setCode(result.getCode());
        dto.setName(result.getName());
        dto.setType(result.getType());
        dto.setPath(result.getPath());

        dto.setDescription(result.getDescription());
        dto.setCreatedAt(result.getCreatedAt());
        dto.setLastModifiedAt(result.getLastModifiedAt());
        return dto;
    }

    /**
     * Converts LocationCoordinates to query LocationCoordinatesDTO.
     */
    private com.ccbsa.wms.location.application.dto.query.LocationCoordinatesDTO toQueryCoordinatesDTO(LocationCoordinates coordinates) {
        return new com.ccbsa.wms.location.application.dto.query.LocationCoordinatesDTO(coordinates.getZone(), coordinates.getAisle(), coordinates.getRack(),
                coordinates.getLevel());
    }

    /**
     * Converts LocationCapacity to LocationCapacityDTO.
     */
    private LocationCapacityDTO toCapacityDTO(LocationCapacity capacity) {
        if (capacity == null) {
            return null;
        }
        return new LocationCapacityDTO(capacity.getCurrentQuantity(), capacity.getMaximumQuantity());
    }

    /**
     * Converts UpdateLocationStatusCommandDTO to UpdateLocationStatusCommand.
     *
     * @param dto        Command DTO
     * @param locationId Location ID string
     * @param tenantId   Tenant identifier string
     * @return UpdateLocationStatusCommand
     */
    public UpdateLocationStatusCommand toUpdateStatusCommand(UpdateLocationStatusCommandDTO dto, String locationId, String tenantId) {
        LocationStatus status;
        try {
            status = LocationStatus.valueOf(dto.getStatus().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Invalid location status: %s", dto.getStatus()));
        }

        return UpdateLocationStatusCommand.builder().locationId(LocationId.of(UUID.fromString(locationId))).tenantId(TenantId.of(tenantId)).status(status).reason(dto.getReason())
                .build();
    }

    /**
     * Converts UpdateLocationStatusResult to UpdateLocationStatusResultDTO.
     *
     * @param result Command result
     * @return UpdateLocationStatusResultDTO
     */
    public UpdateLocationStatusResultDTO toUpdateStatusResultDTO(UpdateLocationStatusResult result) {
        UpdateLocationStatusResultDTO dto = new UpdateLocationStatusResultDTO();
        dto.setLocationId(result.getLocationId().getValueAsString());
        dto.setStatus(result.getStatus());
        dto.setLastModifiedAt(result.getLastModifiedAt());
        return dto;
    }

    /**
     * Converts UpdateLocationCommandDTO to UpdateLocationCommand.
     *
     * @param dto        Command DTO
     * @param locationId Location ID string
     * @param tenantId   Tenant identifier string
     * @return UpdateLocationCommand
     */
    public UpdateLocationCommand toUpdateLocationCommand(UpdateLocationCommandDTO dto, String locationId, String tenantId) {
        UpdateLocationCommand.Builder builder = UpdateLocationCommand.builder().locationId(LocationId.of(UUID.fromString(locationId))).tenantId(TenantId.of(tenantId));

        // Build coordinates from DTO
        LocationCoordinates coordinates = LocationCoordinates.of(dto.getZone() != null && !dto.getZone().trim().isEmpty() ? dto.getZone() : "00",
                dto.getAisle() != null && !dto.getAisle().trim().isEmpty() ? dto.getAisle() : "00", dto.getRack() != null && !dto.getRack().trim().isEmpty() ? dto.getRack() : "00",
                dto.getLevel() != null && !dto.getLevel().trim().isEmpty() ? dto.getLevel() : "00");
        builder.coordinates(coordinates);

        // Set barcode if provided
        if (dto.getBarcode() != null && !dto.getBarcode().trim().isEmpty()) {
            builder.barcode(LocationBarcode.of(dto.getBarcode()));
        }

        // Set description if provided
        if (dto.getDescription() != null && !dto.getDescription().trim().isEmpty()) {
            builder.description(dto.getDescription());
        }

        return builder.build();
    }

    /**
     * Converts AssignLocationsFEFOCommandDTO to AssignLocationsFEFOCommand.
     *
     * @param dto      Command DTO
     * @param tenantId Tenant identifier string
     * @return AssignLocationsFEFOCommand
     */
    public AssignLocationsFEFOCommand toAssignLocationsFEFOCommand(com.ccbsa.wms.location.application.dto.command.AssignLocationsFEFOCommandDTO dto, String tenantId) {
        List<StockItemAssignmentRequest> stockItems = dto.getStockItems().stream()
                .map(item -> StockItemAssignmentRequest.builder().stockItemId(item.getStockItemId()).quantity(item.getQuantity())
                        .expirationDate(item.getExpirationDate() != null ? ExpirationDate.of(item.getExpirationDate()) : null)
                        .classification(item.getClassification() != null ? StockClassification.valueOf(item.getClassification().toUpperCase(Locale.ROOT)) : null).build())
                .collect(Collectors.toList());

        return AssignLocationsFEFOCommand.builder().tenantId(TenantId.of(tenantId)).stockItems(stockItems).build();
    }

    /**
     * Converts parameters to CheckLocationAvailabilityQuery.
     *
     * @param locationId       Location ID string
     * @param tenantId         Tenant identifier string
     * @param requiredQuantity Required quantity
     * @return CheckLocationAvailabilityQuery
     */
    public CheckLocationAvailabilityQuery toCheckLocationAvailabilityQuery(String locationId, String tenantId, Integer requiredQuantity) {
        return CheckLocationAvailabilityQuery.builder().locationId(LocationId.of(UUID.fromString(locationId))).tenantId(TenantId.of(tenantId))
                .requiredQuantity(java.math.BigDecimal.valueOf(requiredQuantity)).build();
    }

    /**
     * Converts LocationAvailabilityResult to LocationAvailabilityQueryResultDTO.
     *
     * @param result Query result
     * @return LocationAvailabilityQueryResultDTO
     */
    public LocationAvailabilityQueryResultDTO toLocationAvailabilityQueryResultDTO(LocationAvailabilityResult result) {
        LocationAvailabilityQueryResultDTO dto = new LocationAvailabilityQueryResultDTO();
        dto.setAvailable(result.isAvailable());
        dto.setHasCapacity(result.hasCapacity());
        dto.setAvailableCapacity(result.getAvailableCapacity() != null ? result.getAvailableCapacity().intValue() : null);
        dto.setReason(result.getReason());
        return dto;
    }
}

