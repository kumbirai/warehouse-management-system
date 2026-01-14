package com.ccbsa.wms.gateway.api.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePickingTaskRequest {
    private String orderId;
    private List<PickingItem> items;
    private String priority;
    private LocalDate dueDate;
}

