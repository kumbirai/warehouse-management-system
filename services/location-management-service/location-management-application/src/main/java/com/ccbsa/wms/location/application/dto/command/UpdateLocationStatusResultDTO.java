package com.ccbsa.wms.location.application.dto.command;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result DTO: UpdateLocationStatusResultDTO
 * <p>
 * Response DTO for location status update operation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class UpdateLocationStatusResultDTO {
    private String locationId;
    private String status;
    private LocalDateTime lastModifiedAt;
}

