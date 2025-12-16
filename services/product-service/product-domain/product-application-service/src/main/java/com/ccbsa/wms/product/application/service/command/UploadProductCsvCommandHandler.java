package com.ccbsa.wms.product.application.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccbsa.common.domain.DomainEvent;
import com.ccbsa.common.domain.valueobject.TenantId;
import com.ccbsa.wms.product.application.service.command.dto.ProductCsvError;
import com.ccbsa.wms.product.application.service.command.dto.ProductCsvRow;
import com.ccbsa.wms.product.application.service.command.dto.UploadProductCsvCommand;
import com.ccbsa.wms.product.application.service.command.dto.UploadProductCsvResult;
import com.ccbsa.wms.product.application.service.port.messaging.ProductEventPublisher;
import com.ccbsa.wms.product.application.service.port.repository.ProductRepository;
import com.ccbsa.wms.product.domain.core.entity.Product;
import com.ccbsa.wms.product.domain.core.exception.BarcodeAlreadyExistsException;
import com.ccbsa.wms.product.domain.core.valueobject.ProductBarcode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductCode;
import com.ccbsa.wms.product.domain.core.valueobject.ProductId;
import com.ccbsa.wms.product.domain.core.valueobject.UnitOfMeasure;

/**
 * Command Handler: UploadProductCsvCommandHandler
 * <p>
 * Handles uploading product master data via CSV file.
 * <p>
 * Responsibilities:
 * - Parse CSV content
 * - Validate each CSV row
 * - Create or update products based on product code
 * - Collect and publish domain events
 * - Return upload result with statistics and errors
 */
@Component
public class UploadProductCsvCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(UploadProductCsvCommandHandler.class);

    private final ProductRepository repository;
    private final ProductEventPublisher eventPublisher;
    private final ProductCsvParser csvParser;

    public UploadProductCsvCommandHandler(
            ProductRepository repository,
            ProductEventPublisher eventPublisher,
            ProductCsvParser csvParser) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.csvParser = csvParser;
    }

    @Transactional
    public UploadProductCsvResult handle(UploadProductCsvCommand command) {
        // 1. Validate command
        validateCommand(command);

        // 2. Parse CSV content
        List<ProductCsvRow> rows;
        try {
            rows = csvParser.parse(command.getCsvContent());
        } catch (IllegalArgumentException e) {
            logger.error("Failed to parse CSV file: {}", e.getMessage());
            throw new IllegalArgumentException(String.format("Invalid CSV format: %s", e.getMessage()), e);
        }

        // 3. Process rows
        int createdCount = 0;
        int updatedCount = 0;
        List<ProductCsvError> errors = new ArrayList<>();
        List<DomainEvent<?>> allEvents = new ArrayList<>();

        for (ProductCsvRow row : rows) {
            try {
                // Validate row data
                validateRow(row, command.getTenantId());

                // Check if product exists by product code
                ProductCode productCode = ProductCode.of(row.getProductCode());
                Optional<Product> existingProduct = repository.findByProductCodeAndTenantId(
                        productCode,
                        command.getTenantId()
                );

                Product product;
                if (existingProduct.isPresent()) {
                    // Update existing product
                    product = existingProduct.get();
                    updateProductFromRow(product, row, command.getTenantId());
                    updatedCount++;
                    logger.debug("Updated product from CSV row {}: {}", row.getRowNumber(), productCode.getValue());
                } else {
                    // Create new product
                    product = createProductFromRow(row, command.getTenantId());
                    createdCount++;
                    logger.debug("Created product from CSV row {}: {}", row.getRowNumber(), productCode.getValue());
                }

                // Persist product
                Product savedProduct = repository.save(product);

                // Collect domain events
                List<DomainEvent<?>> productEvents = new ArrayList<>(savedProduct.getDomainEvents());
                allEvents.addAll(productEvents);
                savedProduct.clearDomainEvents();

            } catch (Exception e) {
                logger.warn("Error processing CSV row {}: {}", row.getRowNumber(), e.getMessage());
                errors.add(ProductCsvError.builder()
                        .rowNumber(row.getRowNumber())
                        .productCode(row.getProductCode())
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        // 4. Publish all events after transaction commit
        if (!allEvents.isEmpty()) {
            publishEventsAfterCommit(allEvents);
        }

        // 5. Build and return result
        return UploadProductCsvResult.builder()
                .totalRows(rows.size())
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .errors(errors)
                .build();
    }

    /**
     * Validates command before execution.
     *
     * @param command Command to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCommand(UploadProductCsvCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId is required");
        }
        if (command.getCsvContent() == null || command.getCsvContent().trim().isEmpty()) {
            throw new IllegalArgumentException("CSV content is required");
        }
    }

    /**
     * Validates a CSV row before processing.
     *
     * @param row      CSV row to validate
     * @param tenantId Tenant identifier
     * @throws IllegalArgumentException if validation fails
     */
    private void validateRow(ProductCsvRow row, TenantId tenantId) {
        // Validate product code format
        try {
            ProductCode.of(row.getProductCode());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid product code format: %s", row.getProductCode()), e
            );
        }

        // Validate primary barcode format
        try {
            ProductBarcode.of(row.getPrimaryBarcode());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid primary barcode format: %s", row.getPrimaryBarcode()), e
            );
        }

        // Validate unit of measure
        try {
            UnitOfMeasure.valueOf(row.getUnitOfMeasure());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid unit of measure: %s", row.getUnitOfMeasure()), e
            );
        }

        // Validate description length
        if (row.getDescription().length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 characters");
        }
    }

    /**
     * Updates an existing Product from CSV row.
     *
     * @param product  Existing product
     * @param row      CSV row
     * @param tenantId Tenant identifier
     */
    private void updateProductFromRow(Product product, ProductCsvRow row, TenantId tenantId) {
        // Update description
        if (!product.getDescription().equals(row.getDescription())) {
            product.updateDescription(row.getDescription());
        }

        // Update primary barcode if changed
        ProductBarcode newPrimaryBarcode = ProductBarcode.of(row.getPrimaryBarcode());
        if (!product.getPrimaryBarcode().getValue().equals(newPrimaryBarcode.getValue())) {
            // Validate uniqueness
            if (repository.existsByBarcodeAndTenantId(newPrimaryBarcode, tenantId)) {
                throw new BarcodeAlreadyExistsException(
                        String.format("Primary barcode already exists: %s", newPrimaryBarcode.getValue())
                );
            }
            product.updatePrimaryBarcode(newPrimaryBarcode);
        }

        // Update unit of measure if changed
        UnitOfMeasure newUnitOfMeasure = UnitOfMeasure.valueOf(row.getUnitOfMeasure());
        if (product.getUnitOfMeasure() != newUnitOfMeasure) {
            product.updateUnitOfMeasure(newUnitOfMeasure);
        }

        // Update secondary barcodes
        // Remove all existing secondary barcodes
        List<ProductBarcode> existingSecondaryBarcodes = new ArrayList<>(product.getSecondaryBarcodes());
        for (ProductBarcode existingBarcode : existingSecondaryBarcodes) {
            product.removeSecondaryBarcode(existingBarcode);
        }

        // Add new secondary barcode if provided
        if (row.getSecondaryBarcode() != null && !row.getSecondaryBarcode().trim().isEmpty()) {
            ProductBarcode secondaryBarcode = ProductBarcode.of(row.getSecondaryBarcode());
            // Validate uniqueness
            if (repository.existsByBarcodeAndTenantId(secondaryBarcode, tenantId)) {
                throw new BarcodeAlreadyExistsException(
                        String.format("Secondary barcode already exists: %s", secondaryBarcode.getValue())
                );
            }
            product.addSecondaryBarcode(secondaryBarcode);
        }

        // Update optional fields
        product.updateOptionalFields(row.getCategory(), row.getBrand());
    }

    /**
     * Creates a new Product from CSV row.
     *
     * @param row      CSV row
     * @param tenantId Tenant identifier
     * @return Created Product aggregate
     */
    private Product createProductFromRow(ProductCsvRow row, TenantId tenantId) {
        // Validate barcode uniqueness for new products
        ProductBarcode primaryBarcode = ProductBarcode.of(row.getPrimaryBarcode());
        if (repository.existsByBarcodeAndTenantId(primaryBarcode, tenantId)) {
            throw new BarcodeAlreadyExistsException(
                    String.format("Primary barcode already exists: %s", primaryBarcode.getValue())
            );
        }

        // Validate secondary barcode uniqueness if provided
        List<ProductBarcode> secondaryBarcodes = new ArrayList<>();
        if (row.getSecondaryBarcode() != null && !row.getSecondaryBarcode().trim().isEmpty()) {
            ProductBarcode secondaryBarcode = ProductBarcode.of(row.getSecondaryBarcode());
            if (repository.existsByBarcodeAndTenantId(secondaryBarcode, tenantId)) {
                throw new BarcodeAlreadyExistsException(
                        String.format("Secondary barcode already exists: %s", secondaryBarcode.getValue())
                );
            }
            secondaryBarcodes.add(secondaryBarcode);
        }

        Product.Builder builder = Product.builder()
                .productId(ProductId.generate())
                .tenantId(tenantId)
                .productCode(ProductCode.of(row.getProductCode()))
                .description(row.getDescription())
                .primaryBarcode(primaryBarcode)
                .unitOfMeasure(UnitOfMeasure.valueOf(row.getUnitOfMeasure()));

        if (!secondaryBarcodes.isEmpty()) {
            builder.secondaryBarcodes(secondaryBarcodes);
        }

        if (row.getCategory() != null && !row.getCategory().trim().isEmpty()) {
            builder.category(row.getCategory());
        }

        if (row.getBrand() != null && !row.getBrand().trim().isEmpty()) {
            builder.brand(row.getBrand());
        }

        return builder.build();
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

