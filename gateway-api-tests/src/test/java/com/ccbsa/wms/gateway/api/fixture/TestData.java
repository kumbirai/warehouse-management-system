package com.ccbsa.wms.gateway.api.fixture;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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

    /**
     * Generates a unique warehouse code by including a UUID component.
     * This ensures uniqueness across test runs and prevents CODE_ALREADY_EXISTS errors.
     * <p>
     * Note: The code may be sanitized and truncated when used as zone coordinates (max 10 chars),
     * but the full code is preserved for location identification.
     */
    public static String warehouseCode() {
        // Use UUID to ensure uniqueness, but keep it readable with a short prefix
        // Format: WH-XX-XXXXXXXX (e.g., WH-16-91B2EA5A)
        String uuid = UUID.randomUUID().toString().substring(0, 8).replace("-", "").toUpperCase();
        return "WH-" + faker.number().digits(2) + "-" + uuid;
    }

    /**
     * Generates a unique zone code by including a UUID component.
     * This ensures uniqueness across test runs and prevents CODE_ALREADY_EXISTS errors.
     * <p>
     * Note: The code may be sanitized and truncated when used as zone coordinates (max 10 chars),
     * but the full code is preserved for location identification.
     */
    public static String zoneCode() {
        // Format: ZONE-X-XXXXXXXX (e.g., ZONE-A-91B2EA5A)
        String uuid = UUID.randomUUID().toString().substring(0, 8).replace("-", "").toUpperCase();
        return "ZONE-" + faker.bothify("?").toUpperCase() + "-" + uuid;
    }

    /**
     * Generates a unique aisle code by including a UUID component.
     * This ensures uniqueness across test runs and prevents CODE_ALREADY_EXISTS errors.
     * <p>
     * Note: The code may be sanitized and truncated when used as aisle coordinates (max 10 chars),
     * but the full code is preserved for location identification.
     */
    public static String aisleCode() {
        // Format: AISLE-XX-XXXXXXXX (e.g., AISLE-01-91B2EA5A)
        String uuid = UUID.randomUUID().toString().substring(0, 8).replace("-", "").toUpperCase();
        return "AISLE-" + faker.number().digits(2) + "-" + uuid;
    }

    /**
     * Generates a unique rack code by including a UUID component.
     * This ensures uniqueness across test runs and prevents CODE_ALREADY_EXISTS errors.
     * <p>
     * Note: The code may be sanitized and truncated when used as rack coordinates (max 10 chars),
     * but the full code is preserved for location identification.
     */
    public static String rackCode() {
        // Format: RACK-XX-XXXXXXXX (e.g., RACK-A1-91B2EA5A)
        String uuid = UUID.randomUUID().toString().substring(0, 8).replace("-", "").toUpperCase();
        return "RACK-" + faker.bothify("?#").toUpperCase() + "-" + uuid;
    }

    /**
     * Generates a unique bin code by including a UUID component.
     * This ensures uniqueness across test runs and prevents CODE_ALREADY_EXISTS errors.
     * <p>
     * Note: The code may be sanitized and truncated when used as level coordinates (max 10 chars),
     * but the full code is preserved for location identification.
     */
    public static String binCode() {
        // Format: BIN-XX-XXXXXXXX (e.g., BIN-01-91B2EA5A)
        String uuid = UUID.randomUUID().toString().substring(0, 8).replace("-", "").toUpperCase();
        return "BIN-" + faker.number().digits(2) + "-" + uuid;
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

