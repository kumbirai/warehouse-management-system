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
     * Generate consignment CSV file.
     */
    public static File generateConsignmentCsv(Path outputPath, int rowCount, String productId, String locationId) throws IOException {
        File csvFile = outputPath.resolve("consignments-test.csv").toFile();

        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write header
            writer.write("productId,locationId,quantity,batchNumber,expirationDate,manufactureDate,supplierReference\n");

            // Write rows
            for (int i = 0; i < rowCount; i++) {
                writer.write(String.format("%s,%s,%d,%s,%s,%s,%s\n",
                        productId,
                        locationId,
                        TestData.stockQuantity(),
                        TestData.batchNumber(),
                        TestData.expirationDate(),
                        TestData.manufactureDate(),
                        TestData.supplierReference()
                ));
            }
        }

        return csvFile;
    }
}

