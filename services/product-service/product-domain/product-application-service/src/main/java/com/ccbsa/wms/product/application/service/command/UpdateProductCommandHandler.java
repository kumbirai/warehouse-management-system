package com.ccbsa.wms.product.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.Description;
import com.ccbsa.wms.product.application.service.command.dto.UpdateProductCommand;
import com.ccbsa.wms.product.application.service.command.dto.UpdateProductResult;
import com.ccbsa.wms.product.application.service.port.messaging.ProductEventPublisher;
import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.exception.BarcodeAlreadyExistsException;
import com.ccbsa.wms.product.domain.core.exception.ProductNotFoundException;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: UpdateProductCommandHandler
 * <p>
 * Handles updating an existing Product aggregate.
 * <p>
 * Responsibilities: - Loads existing product - Validates barcode uniqueness if barcode changed - Updates product aggregate - Persists aggregate - Publishes domain events after
 * transaction commit
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UpdateProductCommandHandler {
    private final ProductRepository repository;
    private final ProductEventPublisher eventPublisher;

    @Transactional
    public UpdateProductResult handle(UpdateProductCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Load existing product
        Product product = repository.findByIdAndTenantId(command.getProductId(), command.getTenantId())
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product not found: %s", command.getProductId().getValueAsString())));

        // 3. Validate primary barcode uniqueness if changed
        if (!product.getPrimaryBarcode().getValue().equals(command.getPrimaryBarcode().getValue())) {
            validateBarcodeUniquenessExcludingProduct(command.getPrimaryBarcode(), command.getTenantId(), command.getProductId());
            product.updatePrimaryBarcode(command.getPrimaryBarcode());
        }

        // 4. Update description if changed
        Description newDescription = Description.of(command.getDescription());
        if (!product.getDescription().equals(newDescription)) {
            product.updateDescription(newDescription);
        }

        // 5. Update unit of measure if changed
        if (product.getUnitOfMeasure() != command.getUnitOfMeasure()) {
            product.updateUnitOfMeasure(command.getUnitOfMeasure());
        }

        // 6. Update secondary barcodes
        // Strategy: Remove all existing secondary barcodes first, then add new ones
        // This ensures we don't have duplicate barcodes in the list
        List<ProductBarcode> existingSecondaryBarcodes = new ArrayList<>(product.getSecondaryBarcodes());

        // Collect barcode values that are being kept (present in both existing and new lists)
        // SecondaryBarcodes should never be null (mapper ensures empty list if null/empty), but defensive check for safety
        List<String> barcodesToKeep = new ArrayList<>();
        if (command.getSecondaryBarcodes() != null && !command.getSecondaryBarcodes().isEmpty()) {
            for (ProductBarcode newBarcode : command.getSecondaryBarcodes()) {
                barcodesToKeep.add(newBarcode.getValue());
            }
        }

        // Remove existing secondary barcodes that are not in the new list
        for (ProductBarcode existingBarcode : existingSecondaryBarcodes) {
            if (!barcodesToKeep.contains(existingBarcode.getValue())) {
                product.removeSecondaryBarcode(existingBarcode);
            }
        }

        // Add new secondary barcodes (only those not already present)
        if (command.getSecondaryBarcodes() != null && !command.getSecondaryBarcodes().isEmpty()) {
            for (ProductBarcode newBarcode : command.getSecondaryBarcodes()) {
                // Only validate and add if not already present (avoid duplicate adds)
                if (!product.hasBarcode(newBarcode.getValue())) {
                    // Validate uniqueness before adding (checks against other products, excluding current product)
                    validateBarcodeUniquenessExcludingProduct(newBarcode, command.getTenantId(), command.getProductId());
                    product.addSecondaryBarcode(newBarcode);
                }
            }
        }

        // 7. Update optional fields
        product.updateOptionalFields(command.getCategory(), command.getBrand());

        // 8. Get domain events BEFORE saving
        List<DomainEvent<?>> domainEvents = new ArrayList<>(product.getDomainEvents());

        // 9. Persist aggregate
        Product savedProduct = repository.save(product);

        // 10. Publish events after transaction commit
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            product.clearDomainEvents();
        }

        // 11. Return result
        return UpdateProductResult.builder().productId(savedProduct.getId()).lastModifiedAt(savedProduct.getLastModifiedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(UpdateProductCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getProductId() == null) {
            throw new IllegalArgumentException("ProductId is required");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        // Description validation is done in static factory method - no need to check null again
        if (command.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        // Validate description length (Description value object will validate, but we validate here for better error messages)
        if (command.getDescription().length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 characters");
        }
        if (command.getPrimaryBarcode() == null) {
            throw new IllegalArgumentException("PrimaryBarcode is required");
        }
        if (command.getUnitOfMeasure() == null) {
            throw new IllegalArgumentException("UnitOfMeasure is required");
        }
    }

    /**
     * Validates that the barcode is unique for the tenant, excluding a specific product.
     * <p>
     * Used when updating a product to allow the same barcode if it belongs to the product being updated.
     *
     * @param barcode          Barcode to validate
     * @param tenantId         Tenant identifier
     * @param excludeProductId Product ID to exclude from the check
     * @throws BarcodeAlreadyExistsException if barcode already exists (excluding the specified product)
     */
    private void validateBarcodeUniquenessExcludingProduct(ProductBarcode barcode, com.ccbsa.common.domain.valueobject.TenantId tenantId,
                                                           com.ccbsa.common.domain.valueobject.ProductId excludeProductId) {
        if (repository.existsByBarcodeAndTenantIdExcludingProduct(barcode, tenantId, excludeProductId)) {
            throw new BarcodeAlreadyExistsException(String.format("Product barcode already exists: %s", barcode.getValue()));
        }
    }

    /**
     * Publishes domain events after transaction commit to avoid race conditions.
     *
     * @param domainEvents Domain events to publish
     */
    private void publishEventsAfterCommit(List<DomainEvent<?>> domainEvents) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    log.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    log.error("Failed to publish domain events after transaction commit", e);
                }
            }
        });
    }
}

