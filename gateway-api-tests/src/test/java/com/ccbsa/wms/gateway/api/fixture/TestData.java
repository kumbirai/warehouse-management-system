package com.ccbsa.wms.gateway.api.fixture;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import com.ccbsa.wms.gateway.api.util.BarcodeGenerator;

import net.datafaker.Faker;

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

    public static String domain() {
        return faker.internet().domainName();
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
        return IntStream.range(0, count).mapToObj(i -> BarcodeGenerator.generateEAN13()).toList();
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

    // Thread-safe counters for generating unique sequential codes
    private static final AtomicInteger warehouseCounter = new AtomicInteger(1);
    private static final AtomicInteger zoneCounter = new AtomicInteger(1);
    private static final AtomicInteger aisleCounter = new AtomicInteger(1);
    private static final AtomicInteger rackCounter = new AtomicInteger(1);
    private static final AtomicInteger binCounter = new AtomicInteger(1);

    /**
     * Generates a unique warehouse code using realistic naming conventions.
     * Format: WH-01, WH-02, etc.
     * This ensures uniqueness across test runs and prevents CODE_ALREADY_EXISTS errors.
     */
    public static String warehouseCode() {
        int num = warehouseCounter.getAndIncrement();
        return String.format("WH-%02d", num);
    }

    /**
     * Generates a unique zone code using realistic naming conventions.
     * Format: ZONE-A, ZONE-B, etc. (or ZONE-01, ZONE-02 if more than 26 zones)
     * This ensures uniqueness across test runs and prevents CODE_ALREADY_EXISTS errors.
     */
    public static String zoneCode() {
        int num = zoneCounter.getAndIncrement();
        if (num <= 26) {
            // Use letters A-Z for first 26 zones
            char letter = (char) ('A' + num - 1);
            return "ZONE-" + letter;
        } else {
            // Use numbers for zones beyond 26
            return String.format("ZONE-%02d", num);
        }
    }

    /**
     * Generates a unique aisle code using realistic naming conventions.
     * Format: AISLE-01, AISLE-02, etc.
     * This ensures uniqueness across test runs and prevents CODE_ALREADY_EXISTS errors.
     */
    public static String aisleCode() {
        int num = aisleCounter.getAndIncrement();
        return String.format("AISLE-%02d", num);
    }

    /**
     * Generates a unique rack code using realistic naming conventions.
     * Format: RACK-A1, RACK-A2, RACK-B1, etc.
     * This ensures uniqueness across test runs and prevents CODE_ALREADY_EXISTS errors.
     */
    public static String rackCode() {
        int num = rackCounter.getAndIncrement();
        // Cycle through letters A-Z and numbers 1-9
        char letter = (char) ('A' + ((num - 1) / 9) % 26);
        int digit = ((num - 1) % 9) + 1;
        return "RACK-" + letter + digit;
    }

    /**
     * Generates a unique bin code using realistic naming conventions.
     * Format: BIN-01, BIN-02, etc.
     * This ensures uniqueness across test runs and prevents CODE_ALREADY_EXISTS errors.
     */
    public static String binCode() {
        int num = binCounter.getAndIncrement();
        return String.format("BIN-%02d", num);
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

