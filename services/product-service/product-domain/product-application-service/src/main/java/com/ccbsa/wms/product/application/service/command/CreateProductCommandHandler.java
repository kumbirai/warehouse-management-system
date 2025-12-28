package com.ccbsa.wms.product.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.Description;
import com.ccbsa.common.domain.valueobject.ProductId;
import com.ccbsa.wms.product.application.service.command.dto.CreateProductCommand;
import com.ccbsa.wms.product.application.service.command.dto.CreateProductResult;
import com.ccbsa.wms.product.application.service.port.messaging.ProductEventPublisher;
import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.exception.BarcodeAlreadyExistsException;
import com.ccbsa.wms.product.domain.core.exception.ProductCodeAlreadyExistsException;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command Handler: CreateProductCommandHandler
 * <p>
 * Handles creation of new Product aggregate.
 * <p>
 * Responsibilities: - Validates product code uniqueness - Validates barcode uniqueness (primary and secondary) - Creates Product aggregate - Persists aggregate - Publishes domain
 * events after transaction commit
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreateProductCommandHandler {
    private final ProductRepository repository;
    private final ProductEventPublisher eventPublisher;

    @Transactional
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "Null checks are defensive for optional DTO fields (category, brand). SpotBugs false "
            + "positive.")
    public CreateProductResult handle(CreateProductCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Validate product code uniqueness
        validateProductCodeUniqueness(command.getProductCode(), command.getTenantId());

        // 3. Validate primary barcode uniqueness
        validateBarcodeUniqueness(command.getPrimaryBarcode(), command.getTenantId());

        // 4. Validate secondary barcodes uniqueness
        // SecondaryBarcodes is validated as non-null in static factory method, but may be empty
        if (!command.getSecondaryBarcodes().isEmpty()) {
            for (ProductBarcode barcode : command.getSecondaryBarcodes()) {
                validateBarcodeUniqueness(barcode, command.getTenantId());
            }
        }

        // 5. Create aggregate using builder
        Product.Builder builder = Product.builder().productId(ProductId.generate()).tenantId(command.getTenantId()).productCode(command.getProductCode())
                .description(Description.of(command.getDescription())).primaryBarcode(command.getPrimaryBarcode()).unitOfMeasure(command.getUnitOfMeasure());

        // Add secondary barcodes if provided
        if (!command.getSecondaryBarcodes().isEmpty()) {
            builder.secondaryBarcodes(command.getSecondaryBarcodes());
        }

        // Set optional fields (category and brand may be null - they're optional)
        // SpotBugs flags these as redundant, but they're defensive checks for optional fields
        @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE") String category = command.getCategory();
        if (category != null && !category.trim().isEmpty()) {
            builder.category(category);
        }
        @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE") String brand = command.getBrand();
        if (brand != null && !brand.trim().isEmpty()) {
            builder.brand(brand);
        }

        Product product = builder.build();

        // 6. Get domain events BEFORE saving (save() returns a new instance without events)
        List<DomainEvent<?>> domainEvents = new ArrayList<>(product.getDomainEvents());

        // 7. Persist aggregate (this returns a new instance from mapper, without domain events)
        Product savedProduct = repository.save(product);

        // 8. Publish events after transaction commit to avoid race conditions
        if (!domainEvents.isEmpty()) {
            publishEventsAfterCommit(domainEvents);
            // Clear events from original product (savedProduct doesn't have them)
            product.clearDomainEvents();
        }

        // 9. Return result (use savedProduct which has updated version from DB)
        return CreateProductResult.builder().productId(savedProduct.getId()).productCode(savedProduct.getProductCode()).description(savedProduct.getDescription().getValue())
                .primaryBarcode(savedProduct.getPrimaryBarcode()).createdAt(savedProduct.getCreatedAt()).build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(CreateProductCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getProductCode() == null) {
            throw new IllegalArgumentException("ProductCode is required");
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
     * Validates that the product code is unique for the tenant.
     *
     * @param productCode Product code to validate
     * @param tenantId    Tenant identifier
     * @throws ProductCodeAlreadyExistsException if product code already exists
     */
    private void validateProductCodeUniqueness(com.ccbsa.wms.product.domain.core.valueobject.ProductCode productCode, com.ccbsa.common.domain.valueobject.TenantId tenantId) {
        if (repository.existsByProductCodeAndTenantId(productCode, tenantId)) {
            throw new ProductCodeAlreadyExistsException(String.format("Product code already exists: %s", productCode.getValue()));
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
            // No active transaction - publish immediately
            log.debug("No active transaction - publishing events immediately");
            eventPublisher.publish(domainEvents);
            return;
        }

        // Register synchronization to publish events after transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    log.debug("Transaction committed - publishing {} domain events", domainEvents.size());
                    eventPublisher.publish(domainEvents);
                } catch (Exception e) {
                    log.error("Failed to publish domain events after transaction commit", e);
                    // Don't throw - transaction already committed, event publishing failure
                    // should be handled by retry mechanisms or dead letter queue
                }
            }
        });
    }
}

