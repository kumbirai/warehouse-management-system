package com.ccbsa.wms.stock.application.dto.command;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Command Result DTO: CreateConsignmentResultDTO
 * <p>
 * API response DTO for consignment creation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConsignmentResultDTO {
    private String consignmentId;
    private String status;
    private LocalDateTime receivedAt;
}

