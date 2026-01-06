package com.ccbsa.wms.location.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command DTO: BlockLocationCommandDTO
 * <p>
 * Request DTO for blocking a location.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockLocationCommandDTO {
    /**
     * Reason for blocking the location.
     * Required field - must not be blank.
     */
    @NotBlank(message = "Block reason is required")
    private String reason;
}

