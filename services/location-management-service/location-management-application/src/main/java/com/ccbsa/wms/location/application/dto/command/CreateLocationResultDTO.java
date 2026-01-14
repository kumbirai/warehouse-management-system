package com.ccbsa.wms.location.application.dto.command;

import java.time.LocalDateTime;

import com.ccbsa.wms.location.application.dto.common.LocationCoordinatesDTO;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result DTO: CreateLocationResultDTO
 * <p>
 * Response DTO returned after creating a location.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Lombok builder stores DTO directly. DTOs are immutable when returned from API.")
public final class CreateLocationResultDTO {
    private String locationId;
    private String code;
    private String name;
    private String type;
    private String path;
    private String barcode;
    private LocationCoordinatesDTO coordinates;
    private String status;
    private LocalDateTime createdAt;
}

