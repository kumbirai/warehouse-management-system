package com.ccbsa.wms.location.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: UpdateLocationStatusCommandDTO
 * <p>
 * Request DTO for updating a location's status.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class UpdateLocationStatusCommandDTO {
    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}

