package com.ccbsa.wms.gateway.api.util;

import com.ccbsa.wms.gateway.api.fixture.TestData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Utility for generating CSV test files.
 */
public class CsvTestDataGenerator {

    /**
     * Generate product CSV file.
     */
    public static File generateProductCsv(Path outputPath, int rowCount) throws IOException {
        File csvFile = outputPath.resolve("products-test.csv").toFile();

        try (FileWriter writer = new FileWriter(csvFile)) {
            // Write header
            writer.write("sku,name,description,primaryBarcode,secondaryBarcodes,category,unitOfMeasure,weight\n");

            // Write rows
            for (int i = 0; i < rowCount; i++) {
                writer.write(String.format("%s,%s,%s,%s,,%s,%s,%.2f\n",
                        TestData.productSKU(),
                        TestData.productName().replace(",", ""),
                        TestData.productDescription().replace(",", ""),
                        TestData.barcode(),
                        TestData.productCategory(),
                        TestData.unitOfMeasure(),
                        TestData.productWeight()
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

