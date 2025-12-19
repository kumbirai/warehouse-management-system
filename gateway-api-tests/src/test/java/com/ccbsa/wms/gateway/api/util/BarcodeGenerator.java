package com.ccbsa.wms.gateway.api.util;

import java.util.Random;

/**
 * Utility for generating valid barcodes (EAN-13, UPC-A).
 */
public class BarcodeGenerator {

    private static final Random random = new Random();

    /**
     * Generate valid EAN-13 barcode with checksum.
     *
     * @return 13-digit EAN-13 barcode
     */
    public static String generateEAN13() {
        // Generate 12 random digits
        long min = 100000000000L;
        long max = 999999999999L;
        long baseNumber = min + (long) (random.nextDouble() * (max - min));
        String base = String.valueOf(baseNumber);

        // Ensure exactly 12 digits
        while (base.length() < 12) {
            base = "0" + base;
        }
        if (base.length() > 12) {
            base = base.substring(0, 12);
        }

        // Calculate checksum
        int checksum = calculateEAN13Checksum(base);

        return base + checksum;
    }

    /**
     * Calculate EAN-13 checksum digit.
     *
     * @param code 12-digit base code
     * @return checksum digit (0-9)
     */
    public static int calculateEAN13Checksum(String code) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(code.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * Generate valid UPC-A barcode with checksum.
     *
     * @return 12-digit UPC-A barcode
     */
    public static String generateUPCA() {
        // Generate 11 random digits
        long min = 10000000000L;
        long max = 99999999999L;
        long baseNumber = min + (long) (random.nextDouble() * (max - min));
        String base = String.valueOf(baseNumber);

        // Ensure exactly 11 digits
        while (base.length() < 11) {
            base = "0" + base;
        }
        if (base.length() > 11) {
            base = base.substring(0, 11);
        }

        // Calculate checksum
        int checksum = calculateUPCAChecksum(base);

        return base + checksum;
    }

    /**
     * Calculate UPC-A checksum digit.
     *
     * @param code 11-digit base code
     * @return checksum digit (0-9)
     */
    public static int calculateUPCAChecksum(String code) {
        int sum = 0;
        for (int i = 0; i < 11; i++) {
            int digit = Character.getNumericValue(code.charAt(i));
            sum += (i % 2 == 0) ? digit * 3 : digit;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * Validate EAN-13 barcode checksum.
     *
     * @param barcode 13-digit EAN-13 barcode
     * @return true if checksum is valid
     */
    public static boolean isValidEAN13(String barcode) {
        if (barcode == null || barcode.length() != 13) {
            return false;
        }

        String base = barcode.substring(0, 12);
        int expectedChecksum = Character.getNumericValue(barcode.charAt(12));
        int actualChecksum = calculateEAN13Checksum(base);

        return expectedChecksum == actualChecksum;
    }
}

