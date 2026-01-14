package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for partial order acceptance.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandlePartialOrderAcceptanceResponse {
    private String returnId;
    private String orderNumber;
    private String returnType;
    private String status;
    private LocalDateTime returnedAt;
}
