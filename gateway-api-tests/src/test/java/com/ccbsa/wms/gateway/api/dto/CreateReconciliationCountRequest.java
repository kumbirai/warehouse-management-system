package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

