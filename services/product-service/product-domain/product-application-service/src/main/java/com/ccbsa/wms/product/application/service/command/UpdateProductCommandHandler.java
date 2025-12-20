package com.ccbsa.wms.product.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.wms.product.application.service.command.dto.UpdateProductCommand;
import com.ccbsa.wms.product.application.service.command.dto.UpdateProductResult;
import com.ccbsa.wms.product.application.service.port.messaging.ProductEventPublisher;
import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.exception.BarcodeAlreadyExistsException;
import com.ccbsa.wms.product.domain.core.exception.ProductNotFoundException;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;

/**
 * Command Handler: UpdateProductCommandHandler
 * <p>
 * Handles updating an existing Product aggregate.
 * <p>
 * Responsibilities: - Loads existing product - Validates barcode uniqueness if barcode changed - Updates product aggregate - Persists aggregate - Publishes domain events after
 * transaction commit
 */
@Component
public class UpdateProductCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(UpdateProductCommandHandler.class);

    private final ProductRepository repository;
    private final ProductEventPublisher eventPublisher;

    public UpdateProductCommandHandler(ProductRepository repository, ProductEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UpdateProductResult handle(UpdateProductCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Load existing product
        Product product = repository.findByIdAndTenantId(command.getProductId(), command.getTenantId())
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product not found: %s", command.getProductId().getValueAsString())));

        // 3. Validate primary barcode uniqueness if changed
        if (!product.getPrimaryBarcode().getValue().equals(command.getPrimaryBarcode().getValue())) {
            validateBarcodeUniqueness(command.getPrimaryBarcode(), command.getTenantId());
            product.updatePrimaryBarcode(command.getPrimaryBarcode());
        }

        // 4. Update description if changed
        if (!product.getDescription().equals(command.getDescription())) {
            product.updateDescription(command.getDescription());
        }

        // 5. Update unit of measure if changed
        if (product.getUnitOfMeasure() != command.getUnitOfMeasure()) {
            product.updateUnitOfMeasure(command.getUnitOfMeasure());
        }

        // 6. Update secondary barcodes
        // Remove all existing secondary barcodes and add new ones
        List<ProductBarcode> existingSecondaryBarcodes = new ArrayList<>(product.getSecondaryBarcodes());
        for (ProductBarcode existingBarcode : existingSecondaryBarcodes) {
            product.removeSecondaryBarcode(existingBarcode);
        }
        if (command.getSecondaryBarcodes() != null && !command.getSecondaryBarcodes().isEmpty()) {
            for (ProductBarcode newBarcode : command.getSecondaryBarcodes()) {
                // Validate uniqueness before adding
                validateBarcodeUniqueness(newBarcode, command.getTenantId());
                product.addSecondaryBarcode(newBarcode);
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
        if (command.getDescription() == null || command.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (command.getPrimaryBarcode() == null) {
            throw new IllegalArgumentException("PrimaryBarcode is required");
        }
        if (command.getUnitOfMeasure() == null) {
            throw new IllegalArgumentException("UnitOfMeasure is required");
        }
    }

    /**
     * Validates that the barcode is unique for the tenant.
     *
     * @param barcode  Barcode to validate
     * @param tenantId Tenant identifier
     * @throws BarcodeAlreadyExistsException if barcode already exists
     */
    private void validateBarcodeUniqueness(ProductBarcode barcode, com.ccbsa.common.domain.valueobject.TenantId tenantId) {
        if (repository.existsByBarcodeAndTenantId(barcode, tenantId)) {
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
            logger.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    logger.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    logger.error("Failed to publish domain events after transaction commit", e);
                }
            }
        });
    }
}

