package com.ccbsa.wms.gateway.api.fixture.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Simple utility to verify the H2 test data repository.
 * Can be run as a standalone main method or from tests.
 */
public class TestDataRepositoryVerification {

    public static void main(String[] args) {
        verifyDatabase();
    }

    public static void verifyDatabase() {
        String url = "jdbc:h2:file:./target/test-data/test-data";
        String user = "sa";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("=== Database Connection Successful ===\n");

            // Show all tables
            System.out.println("=== TABLES ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
                int count = 0;
                while (rs.next()) {
                    System.out.println("- " + rs.getString("TABLE_NAME"));
                    count++;
                }
                if (count == 0) {
                    System.out.println("No tables found (database may be empty)");
                }
            }

            // Count records in each table
            System.out.println("\n=== RECORD COUNTS ===");
            String[] tables = { "test_locations", "test_products", "test_orders", "test_users", "test_consignments",
                    "test_tenants" };
            for (String table : tables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM " + table)) {
                    if (rs.next()) {
                        int count = rs.getInt("count");
                        System.out.println(table + ": " + count + " record(s)");
                    }
                } catch (SQLException e) {
                    System.out.println(table + ": Table does not exist or error - " + e.getMessage());
                }
            }

            // Show sample location data
            System.out.println("\n=== SAMPLE LOCATION DATA (first 10) ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT location_id, location_code, location_type, tenant_id FROM test_locations LIMIT 10")) {
                System.out.println(String.format("%-40s | %-30s | %-15s | %-20s", "Location ID", "Code", "Type", "Tenant ID"));
                System.out.println(
                        "------------------------------------------------------------------------------------------------------------------------");
                int count = 0;
                while (rs.next()) {
                    System.out.println(String.format("%-40s | %-30s | %-15s | %-20s", rs.getString("location_id"),
                            rs.getString("location_code"), rs.getString("location_type"), rs.getString("tenant_id")));
                    count++;
                }
                if (count == 0) {
                    System.out.println("No location data found");
                }
            } catch (SQLException e) {
                System.out.println("Error querying locations: " + e.getMessage());
            }

            // Show sample product data
            System.out.println("\n=== SAMPLE PRODUCT DATA (first 10) ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT product_id, product_code, tenant_id FROM test_products LIMIT 10")) {
                System.out.println(String.format("%-40s | %-30s | %-20s", "Product ID", "Code", "Tenant ID"));
                System.out.println("----------------------------------------------------------------------------------------");
                int count = 0;
                while (rs.next()) {
                    System.out.println(String.format("%-40s | %-30s | %-20s", rs.getString("product_id"),
                            rs.getString("product_code"), rs.getString("tenant_id")));
                    count++;
                }
                if (count == 0) {
                    System.out.println("No product data found");
                }
            } catch (SQLException e) {
                System.out.println("Error querying products: " + e.getMessage());
            }

            // Show location types distribution
            System.out.println("\n=== LOCATION TYPES DISTRIBUTION ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT location_type, COUNT(*) as count FROM test_locations GROUP BY location_type ORDER BY count DESC")) {
                System.out.println("Type      | Count");
                System.out.println("------------------");
                while (rs.next()) {
                    System.out.println(String.format("%-9s | %d", rs.getString("location_type"), rs.getInt("count")));
                }
            } catch (SQLException e) {
                System.out.println("Error querying location types: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
