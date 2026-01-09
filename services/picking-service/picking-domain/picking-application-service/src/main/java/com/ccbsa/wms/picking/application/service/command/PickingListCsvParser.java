package com.ccbsa.wms.picking.application.service.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import com.ccbsa.wms.picking.application.service.command.dto.PickingListCsvRow;

import lombok.extern.slf4j.Slf4j;

/**
 * CSV Parser: PickingListCsvParser
 * <p>
 * Parses CSV files containing picking list data.
 * <p>
 * Responsibilities:
 * - Parse CSV content using Apache Commons CSV
 * - Validate CSV format
 * - Extract picking list data from CSV rows
 * - Handle CSV parsing errors
 */
@Component
@Slf4j
public class PickingListCsvParser {
    private static final int MAX_ROWS = 10000;

    /**
     * Parses CSV content from byte array and returns a list of PickingListCsvRow objects.
     * <p>
     * CSV Format:
     * - First row must be header row
     * - Required columns: LoadNumber, OrderNumber, OrderLineNumber, ProductCode, Quantity, CustomerCode, WarehouseId
     * - Optional columns: CustomerName, Priority, RequestedDeliveryDate
     *
     * @param csvContent CSV file content as byte array
     * @return List of PickingListCsvRow objects
     * @throws IllegalArgumentException if CSV format is invalid
     */
    public List<PickingListCsvRow> parse(byte[] csvContent) {
        if (csvContent == null || csvContent.length == 0) {
            throw new IllegalArgumentException("CSV content cannot be null or empty");
        }

        List<PickingListCsvRow> rows = new ArrayList<>();

        try (InputStream inputStream = new java.io.ByteArrayInputStream(csvContent); InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).setIgnoreEmptyLines(true).build()
                     .parse(reader)) {

            // Validate required headers
            validateHeaders(parser.getHeaderMap().keySet());

            long rowNumber = 1; // Start from 1 (header is row 0)
            for (CSVRecord record : parser) {
                rowNumber++;

                // Check row limit
                if (rowNumber > MAX_ROWS) {
                    throw new IllegalArgumentException(String.format("CSV file exceeds maximum row limit of %d rows", MAX_ROWS));
                }

                try {
                    PickingListCsvRow row = PickingListCsvRow.builder().rowNumber(rowNumber).loadNumber(getValue(record, "LoadNumber")).orderNumber(getValue(record, "OrderNumber"))
                            .orderLineNumber(parseInteger(getValue(record, "OrderLineNumber"))).productCode(getValue(record, "ProductCode"))
                            .quantity(parseDecimal(getValue(record, "Quantity"))).customerCode(getValue(record, "CustomerCode")).customerName(getValue(record, "CustomerName"))
                            .priority(getValue(record, "Priority")).requestedDeliveryDate(getValue(record, "RequestedDeliveryDate")).warehouseId(getValue(record, "WarehouseId"))
                            .build();

                    rows.add(row);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid CSV row {}: {}", rowNumber, e.getMessage());
                    throw new IllegalArgumentException(String.format("Row %d: %s", rowNumber, e.getMessage()), e);
                }
            }

            log.debug("Parsed {} rows from CSV", rows.size());
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
        List<String> requiredHeaders = List.of("LoadNumber", "OrderNumber", "OrderLineNumber", "ProductCode", "Quantity", "CustomerCode", "WarehouseId");
        List<String> missingHeaders = new ArrayList<>();

        for (String requiredHeader : requiredHeaders) {
            boolean found = headers.stream().anyMatch(header -> header.equalsIgnoreCase(requiredHeader));
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
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        } catch (IllegalArgumentException e) {
            // Column not found, return null for optional columns
            return null;
        }
    }

    /**
     * Parses an integer value from CSV record.
     *
     * @param value String value to parse
     * @return Parsed integer
     * @throws IllegalArgumentException if value cannot be parsed
     */
    private int parseInteger(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Integer value is required");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid integer value: %s", value), e);
        }
    }

    /**
     * Parses a decimal value from CSV record.
     *
     * @param value String value to parse
     * @return Parsed decimal as integer (quantity is stored as integer)
     * @throws IllegalArgumentException if value cannot be parsed
     */
    private int parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Quantity value is required");
        }
        try {
            double decimalValue = Double.parseDouble(value.trim());
            if (decimalValue <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            return (int) Math.round(decimalValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid quantity value: %s", value), e);
        }
    }
}
