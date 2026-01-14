package com.ccbsa.wms.returns.application.dto.command;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO: ProcessFullOrderReturnResponseDTO
 * <p>
 * API response DTO for full order return processing.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for full order return processing")
public class ProcessFullOrderReturnResponseDTO {
    @Schema(description = "Return ID", example = "850e8400-e29b-41d4-a716-446655440000")
    private String returnId;

    @Schema(description = "Order number", example = "ORD-2025-001")
    private String orderNumber;

    @Schema(description = "Return type", example = "FULL")
    private String returnType;

    @Schema(description = "Return status", example = "PROCESSED")
    private String status;

    @Schema(description = "Primary return reason", example = "DEFECTIVE")
    private String primaryReturnReason;

    @Schema(description = "Return timestamp")
    private LocalDateTime returnedAt;
}
