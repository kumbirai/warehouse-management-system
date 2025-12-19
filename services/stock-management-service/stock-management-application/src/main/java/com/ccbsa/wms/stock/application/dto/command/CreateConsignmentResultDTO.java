package com.ccbsa.wms.stock.application.dto.command;

import java.time.LocalDateTime;

/**
 * Command Result DTO: CreateConsignmentResultDTO
 * <p>
 * API response DTO for consignment creation.
 */
public class CreateConsignmentResultDTO {
    private String consignmentId;
    private String status;
    private LocalDateTime receivedAt;

    public CreateConsignmentResultDTO() {
    }

    public String getConsignmentId() {
        return consignmentId;
    }

    public void setConsignmentId(String consignmentId) {
        this.consignmentId = consignmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}

