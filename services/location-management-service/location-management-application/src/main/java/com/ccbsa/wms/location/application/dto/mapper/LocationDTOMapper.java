package com.ccbsa.wms.location.application.dto.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.location.application.dto.command.CreateLocationCommandDTO;
import com.ccbsa.wms.location.application.dto.command.CreateLocationResultDTO;
import com.ccbsa.wms.location.application.dto.query.LocationCapacityDTO;
import com.ccbsa.wms.location.application.dto.query.LocationQueryResultDTO;
import com.ccbsa.wms.location.application.service.command.dto.CreateLocationCommand;
import com.ccbsa.wms.location.application.service.command.dto.CreateLocationResult;
import com.ccbsa.wms.location.application.service.query.dto.GetLocationQuery;
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
                .tenantId(TenantId.of(tenantId))
                .coordinates(LocationCoordinates.of(dto.getZone(), dto.getAisle(), dto.getRack(), dto.getLevel()));

        // Set barcode if provided
        if (dto.getBarcode() != null && !dto.getBarcode()
                .trim()
                .isEmpty()) {
            builder.barcode(LocationBarcode.of(dto.getBarcode()));
        }

        // Set description if provided
        if (dto.getDescription() != null && !dto.getDescription()
                .trim()
                .isEmpty()) {
            builder.description(dto.getDescription());
        }

        return builder.build();
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
        dto.setCapacity(toCapacityDTO(result.getCapacity()));
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

