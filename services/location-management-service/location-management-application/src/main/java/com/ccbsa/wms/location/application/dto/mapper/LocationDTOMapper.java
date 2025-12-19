package com.ccbsa.wms.location.application.dto.mapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.dto.command.CreateLocationCommandDTO;
import com.ccbsa.wms.location.application.dto.command.CreateLocationResultDTO;
import com.ccbsa.wms.location.application.dto.query.ListLocationsQueryResultDTO;
import com.ccbsa.wms.location.application.dto.query.LocationCapacityDTO;
import com.ccbsa.wms.location.application.dto.query.LocationQueryResultDTO;
import com.ccbsa.wms.location.application.service.command.dto.CreateLocationCommand;
import com.ccbsa.wms.location.application.service.command.dto.CreateLocationResult;
import com.ccbsa.wms.location.application.service.query.dto.GetLocationQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListLocationsQuery;
import com.ccbsa.wms.location.application.service.query.dto.ListLocationsQueryResult;
import com.ccbsa.wms.location.application.service.query.dto.LocationQueryResult;
import com.ccbsa.wms.location.domain.core.valueobject.LocationBarcode;
import com.ccbsa.wms.location.domain.core.valueobject.LocationCoordinates;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;

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
        CreateLocationCommand.Builder builder = CreateLocationCommand.builder()
                .tenantId(TenantId.of(tenantId));

        // Determine coordinates based on model type
        // Priority: coordinate-based model > hierarchical model > defaults
        LocationCoordinates coordinates;
        if (dto.isCoordinateBasedModel() &&
                (dto.getZone() != null || dto.getAisle() != null || dto.getRack() != null || dto.getLevel() != null)) {
            // Coordinate-based model: use provided coordinates (fill with defaults if partially provided)
            coordinates = LocationCoordinates.of(
                    dto.getZone() != null && !dto.getZone().trim().isEmpty() ? dto.getZone() : "00",
                    dto.getAisle() != null && !dto.getAisle().trim().isEmpty() ? dto.getAisle() : "00",
                    dto.getRack() != null && !dto.getRack().trim().isEmpty() ? dto.getRack() : "00",
                    dto.getLevel() != null && !dto.getLevel().trim().isEmpty() ? dto.getLevel() : "00"
            );
        } else if (dto.isHierarchicalModel()) {
            // Hierarchical model: generate coordinates from location type and code
            coordinates = generateCoordinatesFromHierarchy(dto);
        } else {
            // Fallback: generate default coordinates
            coordinates = generateDefaultCoordinates(dto);
        }

        builder.coordinates(coordinates);

        // Set barcode if provided
        if (dto.getBarcode() != null && !dto.getBarcode()
                .trim()
                .isEmpty()) {
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
     *
     * @param dto Command DTO
     * @return LocationCoordinates
     */
    private LocationCoordinates generateCoordinatesFromHierarchy(CreateLocationCommandDTO dto) {
        String type = dto.getType() != null ? dto.getType().toUpperCase() : "";
        String code = dto.getCode() != null ? dto.getCode() : "";

        // Sanitize code to remove hyphens and other non-alphanumeric characters
        // This ensures barcode generation produces valid alphanumeric strings
        String sanitizedCode = sanitizeForBarcode(code);

        switch (type) {
            case "WAREHOUSE":
                // For warehouses, use sanitized code as zone, generate defaults for others
                return LocationCoordinates.of(
                        sanitizedCode.isEmpty() ? "WH" : sanitizedCode,
                        "00",
                        "00",
                        "00"
                );
            case "ZONE":
                // For zones, use sanitized code as zone identifier
                return LocationCoordinates.of(
                        sanitizedCode.isEmpty() ? "ZONE" : sanitizedCode,
                        "00",
                        "00",
                        "00"
                );
            case "AISLE":
                // For aisles, derive from parent or use sanitized code
                return LocationCoordinates.of(
                        "ZONE", // Will be derived from parent in future
                        sanitizedCode.isEmpty() ? "AISLE" : sanitizedCode,
                        "00",
                        "00"
                );
            case "RACK":
                // For racks, derive from parent hierarchy
                return LocationCoordinates.of(
                        "ZONE", // Will be derived from parent in future
                        "AISLE", // Will be derived from parent in future
                        sanitizedCode.isEmpty() ? "RACK" : sanitizedCode,
                        "00"
                );
            case "BIN":
            case "LEVEL":
                // For bins/levels, derive from parent hierarchy
                return LocationCoordinates.of(
                        "ZONE", // Will be derived from parent in future
                        "AISLE", // Will be derived from parent in future
                        "RACK", // Will be derived from parent in future
                        sanitizedCode.isEmpty() ? "BIN" : sanitizedCode
                );
            default:
                // Default: use sanitized code as zone if available
                return LocationCoordinates.of(
                        sanitizedCode.isEmpty() ? "LOC" : sanitizedCode,
                        "00",
                        "00",
                        "00"
                );
        }
    }

    /**
     * Generates default coordinates when no model is specified.
     *
     * @param dto Command DTO
     * @return LocationCoordinates with default values
     */
    private LocationCoordinates generateDefaultCoordinates(CreateLocationCommandDTO dto) {
        // Use sanitized code if available, otherwise generate defaults
        String code = dto.getCode() != null ? dto.getCode() : "";
        String zone = sanitizeForBarcode(code);
        if (zone.isEmpty()) {
            zone = "DEFAULT";
        }
        return LocationCoordinates.of(zone, "00", "00", "00");
    }

    /**
     * Sanitizes a string for barcode generation by removing non-alphanumeric characters.
     * This ensures that when coordinates are concatenated to form a barcode, the result
     * is valid alphanumeric (8-20 characters as per CCBSA standards).
     *
     * @param value String value to sanitize
     * @return Sanitized string with only alphanumeric characters, uppercase
     */
    private String sanitizeForBarcode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        // Remove all non-alphanumeric characters and convert to uppercase
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    /**
     * Converts CreateLocationResult to CreateLocationResultDTO.
     *
     * @param result Command result
     * @return CreateLocationResultDTO
     */
    public CreateLocationResultDTO toCreateResultDTO(CreateLocationResult result) {
        CreateLocationResultDTO dto = new CreateLocationResultDTO();
        dto.setLocationId(result.getLocationId()
                .getValueAsString());
        dto.setBarcode(result.getBarcode()
                .getValue());
        dto.setCoordinates(toCommandCoordinatesDTO(result.getCoordinates()));
        dto.setStatus(result.getStatus()
                .name());
        dto.setCreatedAt(result.getCreatedAt());

        // Set code/name/type/path from result (path is already generated hierarchically in command handler)
        dto.setCode(result.getCode());
        dto.setName(result.getName());
        dto.setType(result.getType());
        dto.setPath(result.getPath());

        return dto;
    }

    /**
     * Converts LocationCoordinates to command LocationCoordinatesDTO.
     */
    private com.ccbsa.wms.location.application.dto.command.LocationCoordinatesDTO toCommandCoordinatesDTO(LocationCoordinates coordinates) {
        return new com.ccbsa.wms.location.application.dto.command.LocationCoordinatesDTO(coordinates.getZone(), coordinates.getAisle(), coordinates.getRack(),
                coordinates.getLevel());
    }

    /**
     * Converts location ID string and tenant ID to GetLocationQuery.
     *
     * @param locationId Location ID string
     * @param tenantId   Tenant ID string
     * @return GetLocationQuery
     */
    public GetLocationQuery toGetLocationQuery(String locationId, String tenantId) {
        return GetLocationQuery.builder()
                .locationId(LocationId.of(UUID.fromString(locationId)))
                .tenantId(TenantId.of(tenantId))
                .build();
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
    public ListLocationsQuery toListLocationsQuery(String tenantId, Integer page, Integer size,
                                                   String zone, String status, String search) {
        ListLocationsQuery.Builder builder = ListLocationsQuery.builder()
                .tenantId(TenantId.of(tenantId));

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
        List<LocationQueryResultDTO> locationDTOs = result.getLocations().stream()
                .map(this::toQueryResultDTO)
                .collect(Collectors.toList());
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
        dto.setLocationId(result.getLocationId()
                .getValueAsString());
        dto.setBarcode(result.getBarcode()
                .getValue());
        dto.setCoordinates(toQueryCoordinatesDTO(result.getCoordinates()));
        dto.setStatus(result.getStatus()
                .name());

        // Set capacity DTO - getCapacity() will extract Integer value for JSON
        LocationCapacityDTO capacityDTO = toCapacityDTO(result.getCapacity());
        dto.setCapacityDTO(capacityDTO);

        // Set code, name, type, path from query result (now stored in domain)
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
    private LocationCapacityDTO toCapacityDTO(com.ccbsa.wms.location.domain.core.valueobject.LocationCapacity capacity) {
        if (capacity == null) {
            return null;
        }
        return new LocationCapacityDTO(capacity.getCurrentQuantity(), capacity.getMaximumQuantity());
    }
}

