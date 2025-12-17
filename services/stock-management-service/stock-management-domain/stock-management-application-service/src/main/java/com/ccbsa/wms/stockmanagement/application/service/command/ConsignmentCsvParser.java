package com.ccbsa.wms.stockmanagement.application.service.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ccbsa.wms.stockmanagement.application.service.command.dto.ConsignmentCsvRow;

/**
 * CSV Parser: ConsignmentCsvParser
 * <p>
 * Parses CSV files containing stock consignment data.
 * <p>
 * Responsibilities: - Parse CSV content using Apache Commons CSV - Validate CSV format and required columns - Extract consignment data from CSV rows - Handle CSV parsing errors
 */
@Component
public class ConsignmentCsvParser {
    private static final Logger logger = LoggerFactory.getLogger(ConsignmentCsvParser.class);
    private static final int MAX_ROWS = 10000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Required CSV columns.
     */
    private static final Set<String> REQUIRED_COLUMNS = Set.of("ConsignmentReference", "ProductCode", "Quantity", "ReceivedDate", "WarehouseId");

    /**
     * Parses CSV content from InputStream and returns a list of ConsignmentCsvRow objects.
     * <p>
     * CSV Format: - First row must be header row - Required columns: ConsignmentReference, ProductCode, Quantity, ReceivedDate, WarehouseId - Optional columns: ExpirationDate
     *
     * @param inputStream CSV file input stream
     * @return List of ConsignmentCsvRow objects
     * @throws IllegalArgumentException if CSV format is invalid
     * @throws IOException              if reading from stream fails
     */
    public List<ConsignmentCsvRow> parse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("CSV input stream cannot be null");
        }

        List<ConsignmentCsvRow> rows = new ArrayList<>();

        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8); CSVParser parser = CSVFormat.Builder.create()
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
                    ConsignmentCsvRow row = ConsignmentCsvRow.builder()
                            .rowNumber(rowNumber)
                            .consignmentReference(getValue(record, "ConsignmentReference"))
                            .productCode(getValue(record, "ProductCode"))
                            .quantity(parseInteger(getValue(record, "Quantity")))
                            .expirationDate(parseOptionalDate(getValue(record, "ExpirationDate")))
                            .receivedDate(parseDateTime(getValue(record, "ReceivedDate")))
                            .warehouseId(getValue(record, "WarehouseId"))
                            .build();

                    rows.add(row);
                } catch (IllegalArgumentException | DateTimeParseException e) {
                    logger.warn("Invalid CSV row {}: {}", rowNumber, e.getMessage());
                    throw new IllegalArgumentException(String.format("Row %d: %s", rowNumber, e.getMessage()), e);
                }
            }

            logger.debug("Parsed {} rows from CSV", rows.size());
            return rows;
        }
    }

    /**
     * Validates that all required headers are present.
     *
     * @param headers Header names from CSV
     * @throws IllegalArgumentException if required headers are missing
     */
    private void validateHeaders(Set<String> headers) {
        for (String requiredColumn : REQUIRED_COLUMNS) {
            boolean found = headers.stream()
                    .anyMatch(header -> header.equalsIgnoreCase(requiredColumn));
            if (!found) {
                throw new IllegalArgumentException(String.format("Required column '%s' is missing from CSV header", requiredColumn));
            }
        }
    }

    /**
     * Gets a value from CSV record by column name (case-insensitive).
     *
     * @param record     CSV record
     * @param columnName Column name
     * @return Value or null if not found
     */
    private String getValue(CSVRecord record, String columnName) {
        try {
            String value = record.get(columnName);
            return value != null && !value.trim()
                    .isEmpty() ? value.trim() : null;
        } catch (IllegalArgumentException e) {
            // Column not found - try case-insensitive search
            for (String header : record.getParser()
                    .getHeaderMap()
                    .keySet()) {
                if (header.equalsIgnoreCase(columnName)) {
                    String value = record.get(header);
                    return value != null && !value.trim()
                            .isEmpty() ? value.trim() : null;
                }
            }
            return null;
        }
    }

    /**
     * Parses an integer value.
     *
     * @param value String value
     * @return Parsed integer
     * @throws IllegalArgumentException if value cannot be parsed
     */
    private int parseInteger(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("Quantity is required");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid quantity format: %s", value), e);
        }
    }

    /**
     * Parses an optional date value.
     *
     * @param value String value (can be null or empty)
     * @return Parsed LocalDate or null
     * @throws DateTimeParseException if value cannot be parsed
     */
    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(String.format("Invalid date format: %s", value), value, 0, e);
        }
    }

    /**
     * Parses a date-time value.
     *
     * @param value String value
     * @return Parsed LocalDateTime
     * @throws IllegalArgumentException if value is null or empty
     * @throws DateTimeParseException   if value cannot be parsed
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim()
                .isEmpty()) {
            throw new IllegalArgumentException("ReceivedDate is required");
        }
        String trimmed = value.trim();
        try {
            // Try ISO date-time format first
            return LocalDateTime.parse(trimmed, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                // Try ISO date format (assume midnight)
                return LocalDate.parse(trimmed, DATE_FORMATTER)
                        .atStartOfDay();
            } catch (DateTimeParseException e2) {
                throw new DateTimeParseException(String.format("Invalid date-time format: %s", value), value, 0, e2);
            }
        }
    }
}

