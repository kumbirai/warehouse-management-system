package com.ccbsa.wms.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReconciliationCountRequest {
    private String locationId;
    private String countType;
    private LocalDate scheduledDate;
    private String assignedTo;
}

