package com.ccbsa.wms.stockmanagement.application.dto.command;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Command DTO: ValidateConsignmentCommandDTO
 * <p>
 * API request DTO for validating consignment data.
 */
public class ValidateConsignmentCommandDTO {
    @NotBlank(message = "Consignment reference is required")
    private String consignmentReference;

    @NotBlank(message = "Warehouse ID is required")
    private String warehouseId;

    @NotNull(message = "Received date is required")
    private LocalDateTime receivedAt;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<CreateConsignmentCommandDTO.ConsignmentLineItemDTO> lineItems;

    public ValidateConsignmentCommandDTO() {
    }

    public String getConsignmentReference() {
        return consignmentReference;
    }

    public void setConsignmentReference(String consignmentReference) {
        this.consignmentReference = consignmentReference;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public List<CreateConsignmentCommandDTO.ConsignmentLineItemDTO> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<CreateConsignmentCommandDTO.ConsignmentLineItemDTO> lineItems) {
        this.lineItems = lineItems;
    }
}

