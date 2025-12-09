package com.ccbsa.wms.gateway.api.fixture;

import java.util.Locale;

import com.github.javafaker.Faker;

import lombok.extern.slf4j.Slf4j;

/**
 * Test data management singleton for gateway API integration tests.
 *
 * <p>This class provides:
 * <ul>
 *   <li>Single test tenant ID per test suite run</li>
 *   <li>Faker instance for realistic data generation</li>
 *   <li>Factory methods for generating user data</li>
 *   <li>Default password constant for test users</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * TestData testData = TestData.getInstance();
 * String email = testData.generateEmail();
 * String firstName = testData.generateFirstName();
 * </pre>
 */
@Slf4j
public final class TestData {

    // Constants
    public static final String DEFAULT_PASSWORD = "Password123@";
    // Singleton instance
    private static final TestData INSTANCE = new TestData();
    private static final String EMAIL_DOMAIN = "wmstest.com";

    // Test tenant ID - generated once per test suite run
    private final String testTenantId;

    // Faker instance for data generation
    private final Faker faker;

    /**
     * Private constructor for singleton pattern.
     * Initializes test tenant ID and Faker instance.
     */
    private TestData() {
        this.faker = new Faker(Locale.US);
        this.testTenantId = generateKebabCasePassphrase();
        log.info("TestData initialized with tenant ID: {}", testTenantId);
    }

    /**
     * Generates a three-word kebab-case passphrase.
     * Format: word1-word2-word3
     *
     * @return Three-word kebab-case passphrase
     */
    private String generateKebabCasePassphrase() {
        String word1 = faker.lorem().word().toLowerCase().replaceAll("[^a-z]", "");
        String word2 = faker.lorem().word().toLowerCase().replaceAll("[^a-z]", "");
        String word3 = faker.lorem().word().toLowerCase().replaceAll("[^a-z]", "");
        return String.format("%s-%s-%s", word1, word2, word3);
    }

    /**
     * Gets the singleton instance.
     *
     * @return TestData singleton instance
     */
    public static TestData getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the test tenant ID for this test suite run.
     * This ID is generated once when TestData is first accessed.
     *
     * @return Test tenant ID (three-word kebab-case passphrase)
     */
    public String getTestTenantId() {
        return testTenantId;
    }

    /**
     * Gets the Faker instance for custom data generation.
     *
     * @return Faker instance
     */
    public Faker getFaker() {
        return faker;
    }

    /**
     * Generates a unique email address.
     * Format: firstname.lastname.timestamp@wmstest.com
     *
     * @return Unique email address
     */
    public String generateEmail() {
        String firstName = faker.name().firstName().toLowerCase().replaceAll("[^a-z]", "");
        String lastName = faker.name().lastName().toLowerCase().replaceAll("[^a-z]", "");
        long timestamp = System.currentTimeMillis();
        return String.format("%s.%s.%d@%s", firstName, lastName, timestamp, EMAIL_DOMAIN);
    }

    /**
     * Generates a unique username.
     * Format: firstname_lastname_timestamp
     *
     * @return Unique username
     */
    public String generateUsername() {
        String firstName = faker.name().firstName().toLowerCase().replaceAll("[^a-z]", "");
        String lastName = faker.name().lastName().toLowerCase().replaceAll("[^a-z]", "");
        long timestamp = System.currentTimeMillis();
        return String.format("%s_%s_%d", firstName, lastName, timestamp);
    }

    /**
     * Generates a realistic first name.
     *
     * @return First name
     */
    public String generateFirstName() {
        return faker.name().firstName();
    }

    /**
     * Generates a realistic last name.
     *
     * @return Last name
     */
    public String generateLastName() {
        return faker.name().lastName();
    }

    /**
     * Generates a realistic phone number in E.164 format.
     *
     * @return Phone number (e.g., "+1-555-123-4567")
     */
    public String generatePhoneNumber() {
        return faker.phoneNumber().phoneNumber();
    }

    /**
     * Generates a realistic company name.
     *
     * @return Company name
     */
    public String generateCompanyName() {
        return faker.company().name();
    }

    /**
     * Generates a realistic street address.
     *
     * @return Street address
     */
    public String generateStreetAddress() {
        return faker.address().streetAddress();
    }

    /**
     * Generates a realistic city name.
     *
     * @return City name
     */
    public String generateCity() {
        return faker.address().city();
    }

    /**
     * Gets the default password for test users.
     *
     * @return Default password
     */
    public String getDefaultPassword() {
        return DEFAULT_PASSWORD;
    }

    /**
     * Generates a unique tenant ID.
     * Format: word1-word2-word3-timestamp
     * Follows the same kebab-case pattern as the test tenant ID but with uniqueness.
     *
     * @return Unique tenant ID (kebab-case passphrase with timestamp)
     */
    public String generateUniqueTenantId() {
        return generateKebabCasePassphrase();
    }
}
