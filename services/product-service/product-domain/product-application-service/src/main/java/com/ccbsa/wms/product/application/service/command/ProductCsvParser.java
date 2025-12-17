package com.ccbsa.wms.product.application.service.command;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ccbsa.wms.product.application.service.command.dto.ProductCsvRow;

/**
 * CSV Parser: ProductCsvParser
 * <p>
 * Parses CSV files containing product master data.
 * <p>
 * Responsibilities: - Parse CSV content using Apache Commons CSV - Validate CSV format - Extract product data from CSV rows - Handle CSV parsing errors
 */
@Component
public class ProductCsvParser {
    private static final Logger logger = LoggerFactory.getLogger(ProductCsvParser.class);
    private static final int MAX_ROWS = 10000;

    /**
     * Parses CSV content and returns a list of ProductCsvRow objects.
     * <p>
     * CSV Format: - First row must be header row - Required columns: product_code, description, primary_barcode, unit_of_measure - Optional columns: secondary_barcode, category,
     * brand
     *
     * @param csvContent CSV file content as string
     * @return List of ProductCsvRow objects
     * @throws IllegalArgumentException if CSV format is invalid
     */
    public List<ProductCsvRow> parse(String csvContent) {
        if (csvContent == null || csvContent.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("CSV content cannot be null or empty");
        }

        List<ProductCsvRow> rows = new ArrayList<>();

        try (StringReader reader = new StringReader(csvContent); CSVParser parser = CSVFormat.Builder.create()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build()
                .parse(reader)) {

            // Validate required headers
            validateHeaders(parser.getHeaderMap()
                    .keySet());

            long rowNumber = 1; // Start from 1 (header is row 0)
            for (CSVRecord record : parser) {
                rowNumber++;

                // Check row limit
                if (rowNumber > MAX_ROWS) {
                    throw new IllegalArgumentException(String.format("CSV file exceeds maximum row limit of %d rows", MAX_ROWS));
                }

                try {
                    ProductCsvRow row = ProductCsvRow.builder()
                            .rowNumber(rowNumber)
                            .productCode(getValue(record, "product_code"))
                            .description(getValue(record, "description"))
                            .primaryBarcode(getValue(record, "primary_barcode"))
                            .unitOfMeasure(getValue(record, "unit_of_measure"))
                            .secondaryBarcode(getValue(record, "secondary_barcode"))
                            .category(getValue(record, "category"))
                            .brand(getValue(record, "brand"))
                            .build();

                    rows.add(row);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid CSV row {}: {}", rowNumber, e.getMessage());
                    throw new IllegalArgumentException(String.format("Row %d: %s", rowNumber, e.getMessage()), e);
                }
            }

            logger.debug("Parsed {} rows from CSV", rows.size());
            return rows;

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse CSV content", e);
        }
    }

    /**
     * Validates that required CSV headers are present.
     *
     * @param headers Set of header names from CSV
     * @throws IllegalArgumentException if required headers are missing
     */
    private void validateHeaders(Set<String> headers) {
        List<String> requiredHeaders = List.of("product_code", "description", "primary_barcode", "unit_of_measure");
        List<String> missingHeaders = new ArrayList<>();

        for (String requiredHeader : requiredHeaders) {
            boolean found = headers.stream()
                    .anyMatch(header -> header.equalsIgnoreCase(requiredHeader));
            if (!found) {
                missingHeaders.add(requiredHeader);
            }
        }

        if (!missingHeaders.isEmpty()) {
            throw new IllegalArgumentException(String.format("Missing required CSV headers: %s", String.join(", ", missingHeaders)));
        }
    }

    /**
     * Gets a value from CSV record, handling case-insensitive column names.
     *
     * @param record     CSV record
     * @param columnName Column name (case-insensitive)
     * @return Column value, or null if not present
     */
    private String getValue(CSVRecord record, String columnName) {
        try {
            String value = record.get(columnName);
            return (value != null && !value.trim()
                    .isEmpty()) ? value.trim() : null;
        } catch (IllegalArgumentException e) {
            // Column not found, return null for optional columns
            return null;
        }
    }
}

