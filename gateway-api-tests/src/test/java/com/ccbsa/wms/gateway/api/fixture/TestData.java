package com.ccbsa.wms.gateway.api.fixture;

import com.ccbsa.wms.gateway.api.util.BarcodeGenerator;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Central test data factory providing realistic test data for all tests.
 * Uses Faker for data generation with consistent locale settings.
 */
public class TestData {

    private static final Faker faker = new Faker(new Locale("en", "US"));

    // ==================== TENANT DATA ====================

    public static String tenantId() {
        return "TENANT-" + faker.number().digits(6);
    }

    public static String tenantName() {
        return faker.company().name();
    }

    public static String tenantEmail() {
        return faker.internet().emailAddress();
    }

    // ==================== USER DATA ====================

    public static String username() {
        return faker.name().username();
    }

    public static String email() {
        return faker.internet().emailAddress();
    }

    public static String firstName() {
        return faker.name().firstName();
    }

    public static String lastName() {
        return faker.name().lastName();
    }

    public static String password() {
        return "Password123@";
    }

    // ==================== PRODUCT DATA ====================

    public static String productSKU() {
        return "SKU-" + faker.number().digits(6);
    }

    public static String productName() {
        return faker.commerce().productName();
    }

    public static String productDescription() {
        return faker.lorem().sentence();
    }

    public static String barcode() {
        return BarcodeGenerator.generateEAN13();
    }

    public static List<String> secondaryBarcodes(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> BarcodeGenerator.generateEAN13())
                .toList();
    }

    public static String productCategory() {
        return faker.options().option("BEVERAGES", "SNACKS", "DAIRY", "FROZEN", "FRESH_PRODUCE");
    }

    public static String unitOfMeasure() {
        // Valid UnitOfMeasure enum values: EA, CS, PK, BOX, PAL
        return faker.options().option("EA", "CS", "PK", "BOX", "PAL");
    }

    public static double productWeight() {
        return faker.number().randomDouble(2, 0, 50);
    }

    // ==================== LOCATION DATA ====================

    public static String warehouseCode() {
        return "WH-" + faker.number().digits(2);
    }

    public static String zoneCode() {
        return "ZONE-" + faker.bothify("?").toUpperCase();
    }

    public static String aisleCode() {
        return "AISLE-" + faker.number().digits(2);
    }

    public static String rackCode() {
        return "RACK-" + faker.bothify("?#").toUpperCase();
    }

    public static String binCode() {
        return "BIN-" + faker.number().digits(2);
    }

    public static int locationCapacity(String type) {
        return switch (type) {
            case "WAREHOUSE" -> faker.number().numberBetween(5000, 20000);
            case "ZONE" -> faker.number().numberBetween(1000, 5000);
            case "AISLE" -> faker.number().numberBetween(200, 1000);
            case "RACK" -> faker.number().numberBetween(50, 200);
            case "BIN" -> faker.number().numberBetween(10, 50);
            default -> 100;
        };
    }

    // ==================== STOCK DATA ====================

    public static int stockQuantity() {
        return faker.number().numberBetween(10, 500);
    }

    public static String batchNumber() {
        return "BATCH-" + faker.number().digits(6);
    }

    public static LocalDate manufactureDate() {
        return LocalDate.now().minusDays(faker.number().numberBetween(15, 90));
    }

    public static LocalDate expirationDate() {
        return LocalDate.now().plusMonths(faker.number().numberBetween(3, 12));
    }

    public static String supplierReference() {
        return "PO-" + faker.number().digits(5);
    }

    // ==================== ORDER DATA ====================

    public static String orderId() {
        return "ORDER-" + faker.number().digits(5);
    }

    public static String customerId() {
        return "CUST-" + faker.number().digits(4);
    }

    // ==================== COMMON ====================

    public static Faker faker() {
        return faker;
    }
}

