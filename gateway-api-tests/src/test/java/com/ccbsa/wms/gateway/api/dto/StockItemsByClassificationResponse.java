package com.ccbsa.wms.gateway.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO matching StockItemsByClassificationResponseDTO from the service.
 * Wrapper DTO for stock items by classification query response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemsByClassificationResponse {
    private List<StockItemResponse> stockItems;
}
