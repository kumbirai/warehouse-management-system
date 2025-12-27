package com.ccbsa.wms.gateway.api.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import com.ccbsa.wms.gateway.api.fixture.TestData;

/**
 * Utility for generating CSV test files.
 */
public class CsvTestDataGenerator {

    /**
     * Generate product CSV file.
     * Uses snake_case headers as expected by ProductCsvParser:
     * - Required: product_code, description, primary_barcode, unit_of_measure
     * - Optional: secondary_barcode, category, brand
     */
    public static File generateProductCsv(Path outputPath, int rowCount) throws IOException {
        File csvFile = outputPath.resolve("products-test.csv").toFile();

        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write header with snake_case column names matching ProductCsvParser expectations
            writer.write("product_code,description,primary_barcode,unit_of_measure,category,brand\n");

            // Write rows
            for (int i = 0; i < rowCount; i++) {
                String productCode = TestData.productSKU();
                String description = TestData.productDescription().replace(",", "").replace("\n", " ").trim();
                // Ensure description is not empty
                if (description == null || description.isEmpty()) {
                    description = "Test Product Description";
                }
                String barcode = TestData.barcode();
                String unitOfMeasure = TestData.unitOfMeasure();
                String category = TestData.productCategory();
                
                writer.write(String.format("%s,%s,%s,%s,%s,\n",
                        productCode,
                        description,
                        barcode,
                        unitOfMeasure,
                        category
                ));
            }
        }

        return csvFile;
    }

    /**
     * Generate consignment CSV file with new format.
     * Required columns: ConsignmentReference, ProductCode, Quantity, ReceivedDate, WarehouseId
     * Optional columns: ExpirationDate
     */
    public static File generateConsignmentCsv(Path outputPath, int rowCount, String productCode, String warehouseId) throws IOException {
        File csvFile = outputPath.resolve("consignments-test.csv").toFile();

        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write header with required columns
            writer.write("ConsignmentReference,ProductCode,Quantity,ReceivedDate,WarehouseId,ExpirationDate\n");

            // Write rows - each row is a line item, multiple rows with same ConsignmentReference form one consignment
            String consignmentRef = "CONS-TEST-" + System.currentTimeMillis();
            java.time.LocalDateTime receivedDate = java.time.LocalDateTime.now();
            
            for (int i = 0; i < rowCount; i++) {
                // Use same consignment reference for all rows (single consignment with multiple line items)
                // Or use different reference for each row (multiple consignments)
                String currentConsignmentRef = rowCount == 1 ? consignmentRef : consignmentRef + "-" + i;
                String expirationDate = TestData.expirationDate() != null ? TestData.expirationDate().toString() : "";
                
                writer.write(String.format("%s,%s,%d,%s,%s,%s\n",
                        currentConsignmentRef,
                        productCode,
                        TestData.stockQuantity(),
                        receivedDate.toString(),
                        warehouseId,
                        expirationDate
                ));
            }
        }

        return csvFile;
    }
}

