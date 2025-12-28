package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListStockAllocationsQueryResultDTO {
    private List<StockAllocationQueryDTO> allocations;
    private int totalCount;
    private int page;
    private int size;
    private int totalPages;
}

