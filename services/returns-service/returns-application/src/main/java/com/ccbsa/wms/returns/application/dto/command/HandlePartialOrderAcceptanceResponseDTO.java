package com.ccbsa.wms.returns.application.dto.command;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO: HandlePartialOrderAcceptanceResponseDTO
 * <p>
 * API response DTO for partial order acceptance.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for partial order acceptance")
public class HandlePartialOrderAcceptanceResponseDTO {
    @Schema(description = "Return ID", example = "850e8400-e29b-41d4-a716-446655440000")
    private String returnId;

    @Schema(description = "Order number", example = "ORD-2025-001")
    private String orderNumber;

    @Schema(description = "Return type", example = "PARTIAL")
    private String returnType;

    @Schema(description = "Return status", example = "INITIATED")
    private String status;

    @Schema(description = "Return timestamp")
    private LocalDateTime returnedAt;
}
