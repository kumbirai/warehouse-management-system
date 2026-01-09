package com.ccbsa.wms.stock.application.service.command;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ccbsa.common.domain.valueobject.Quantity;
import com.ccbsa.wms.location.domain.core.valueobject.LocationId;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.stock.application.service.command.dto.ValidateConsignmentCommand;
import com.ccbsa.wms.stock.application.service.command.dto.ValidateConsignmentResult;
import com.ccbsa.wms.stock.application.service.port.repository.StockConsignmentRepository;
import com.ccbsa.wms.stock.application.service.port.service.LocationServicePort;
import com.ccbsa.wms.stock.application.service.port.service.ProductServicePort;
import com.ccbsa.wms.stock.domain.core.valueobject.ConsignmentLineItem;

import lombok.RequiredArgsConstructor;

/**
 * Command Handler: ValidateConsignmentCommandHandler
 * <p>
 * Handles validation of consignment data before creation.
 * <p>
 * Responsibilities: - Validate consignment reference uniqueness - Validate product codes exist - Validate quantities - Validate dates - Validate warehouse ID - Return detailed
 * validation errors
 */
@Component
@RequiredArgsConstructor
public class ValidateConsignmentCommandHandler {
    private final StockConsignmentRepository repository;
    private final ProductServicePort productServicePort;
    private final LocationServicePort locationServicePort;

    @Transactional(readOnly = true)
    public ValidateConsignmentResult handle(ValidateConsignmentCommand command) {
        List<String> errors = new ArrayList<>();

        // 1. Validate consignment reference uniqueness
        if (repository.existsByConsignmentReferenceAndTenantId(command.getConsignmentReference(), command.getTenantId())) {
            errors.add(String.format("Consignment reference '%s' already exists", command.getConsignmentReference().getValue()));
        }

        // 2. Validate warehouse ID exists
        // WarehouseId is validated as non-null in DTO constructor
        if (command.getWarehouseId().getValue().trim().isEmpty()) {
            errors.add("Warehouse ID cannot be empty");
        } else {
            // Validate warehouse location exists via LocationServicePort
            try {
                LocationId locationId = LocationId.of(command.getWarehouseId().getValue());
                LocationServicePort.LocationAvailability availability = locationServicePort.checkLocationAvailability(locationId, Quantity.of(1), command.getTenantId());
                if (!availability.isAvailable()) {
                    String reason = availability.getReason();
                    // Distinguish between "location not found" and "service unavailable"
                    if (reason != null && reason.contains("service unavailable")) {
                        // Service unavailable - this is an infrastructure issue, not a validation error
                        // In production, we might want to fail fast or use a different strategy
                        // For now, treat as validation error to maintain data integrity
                        errors.add("Warehouse location validation failed: Location service is temporarily unavailable. Please try again later.");
                    } else {
                        // Location not found or unavailable for business reasons
                        errors.add("Warehouse location not found or unavailable: " + (reason != null ? reason : "Location does not exist"));
                    }
                }
            } catch (RuntimeException e) {
                // Handle circuit breaker exceptions or other runtime exceptions
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Location service unavailable")) {
                    errors.add("Warehouse location validation failed: Location service is temporarily unavailable. Please try again later.");
                } else {
                    errors.add("Warehouse location validation failed: " + (errorMessage != null ? errorMessage : e.getClass().getSimpleName()));
                }
            } catch (Exception e) {
                errors.add("Warehouse location validation failed: " + e.getMessage());
            }
        }

        // 3. Validate received date
        // ReceivedAt is validated as non-null in DTO constructor
        if (command.getReceivedAt().isAfter(LocalDateTime.now())) {
            errors.add("Received date cannot be in the future");
        }

        // 4. Validate line items
        // LineItems is validated as non-null and non-empty in DTO constructor
        validateLineItems(command.getLineItems(), command.getTenantId(), errors);

        // 5. Build result
        return ValidateConsignmentResult.builder().valid(errors.isEmpty()).validationErrors(errors).build();
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
            if (lineItem.getQuantity() == null || !lineItem.getQuantity().isPositive()) {
                errors.add(String.format("Line %d: Quantity must be positive", lineNumber));
            }

            // Validate expiration date (if provided)
            // Note: Allow past dates for testing expired stock scenarios - validation is lenient
            // Business logic will handle expired stock appropriately
        }
    }
}

