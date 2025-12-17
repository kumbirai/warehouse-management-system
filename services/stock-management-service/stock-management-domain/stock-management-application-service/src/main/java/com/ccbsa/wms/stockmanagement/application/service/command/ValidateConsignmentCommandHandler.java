package com.ccbsa.wms.stockmanagement.application.service.command;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.stockmanagement.application.service.command.dto.ValidateConsignmentCommand;
import com.ccbsa.wms.stockmanagement.application.service.command.dto.ValidateConsignmentResult;
import com.ccbsa.wms.stockmanagement.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stockmanagement.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stockmanagement.domain.core.valueobject.ConsignmentLineItem;

/**
 * Command Handler: ValidateConsignmentCommandHandler
 * <p>
 * Handles validation of consignment data before creation.
 * <p>
 * Responsibilities: - Validate consignment reference uniqueness - Validate product codes exist - Validate quantities - Validate dates - Validate warehouse ID - Return detailed
 * validation errors
 */
@Component
public class ValidateConsignmentCommandHandler {
    private final StockConsignmentRepository repository;
    private final ProductServicePort productServicePort;

    public ValidateConsignmentCommandHandler(StockConsignmentRepository repository, ProductServicePort productServicePort) {
        this.repository = repository;
        this.productServicePort = productServicePort;
    }

    @Transactional(readOnly = true)
    public ValidateConsignmentResult handle(ValidateConsignmentCommand command) {
        List<String> errors = new ArrayList<>();

        // 1. Validate consignment reference uniqueness
        if (repository.existsByConsignmentReferenceAndTenantId(command.getConsignmentReference(), command.getTenantId())) {
            errors.add(String.format("Consignment reference '%s' already exists", command.getConsignmentReference()
                    .getValue()));
        }

        // 2. Validate warehouse ID (basic validation - could be enhanced with warehouse service)
        if (command.getWarehouseId() == null || command.getWarehouseId()
                .getValue()
                .trim()
                .isEmpty()) {
            errors.add("Warehouse ID is required");
        }

        // 3. Validate received date
        if (command.getReceivedAt() == null) {
            errors.add("Received date is required");
        } else if (command.getReceivedAt()
                .isAfter(LocalDateTime.now())) {
            errors.add("Received date cannot be in the future");
        }

        // 4. Validate line items
        if (command.getLineItems() == null || command.getLineItems()
                .isEmpty()) {
            errors.add("At least one line item is required");
        } else {
            validateLineItems(command.getLineItems(), command.getTenantId(), errors);
        }

        // 5. Build result
        return ValidateConsignmentResult.builder()
                .valid(errors.isEmpty())
                .validationErrors(errors)
                .build();
    }

    /**
     * Validates all line items.
     *
     * @param lineItems List of line items to validate
     * @param tenantId  Tenant identifier
     * @param errors    List to add validation errors to
     */
    private void validateLineItems(List<ConsignmentLineItem> lineItems, com.ccbsa.common.domain.valueobject.TenantId tenantId, List<String> errors) {
        for (int i = 0; i < lineItems.size(); i++) {
            ConsignmentLineItem lineItem = lineItems.get(i);
            int lineNumber = i + 1;

            // Validate product code exists
            ProductCode productCode = lineItem.getProductCode();
            var productInfo = productServicePort.getProductByCode(productCode, tenantId);
            if (productInfo.isEmpty()) {
                errors.add(String.format("Line %d: Product code '%s' not found", lineNumber, productCode.getValue()));
            }

            // Validate quantity
            if (lineItem.getQuantity() <= 0) {
                errors.add(String.format("Line %d: Quantity must be positive", lineNumber));
            }

            // Validate expiration date (if provided)
            if (lineItem.hasExpirationDate() && lineItem.getExpirationDate()
                    .isBefore(LocalDate.now())) {
                errors.add(String.format("Line %d: Expiration date cannot be in the past", lineNumber));
            }
        }
    }
}

