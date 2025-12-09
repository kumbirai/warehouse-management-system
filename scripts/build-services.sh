#!/bin/bash

set -e

echo "Building all services..."

# Build common modules first
echo "Building common modules..."
echo "  - common-domain"
echo "  - common-messaging"
echo "  - common-application"
echo "  - common-infrastructure"
echo "  - common-security"
echo "  - common-keycloak"
mvn clean install -pl common -am -DskipTests

# Build all services
echo ""
echo "Building services..."
echo "  - gateway-service"
echo "  - user-service"
echo "  - tenant-service"
echo "  - notification-service"
echo "  - stock-management-service"
echo "  - location-management-service"
echo "  - product-service"
echo "  - picking-service"
echo "  - returns-service"
echo "  - reconciliation-service"
echo "  - integration-service"
mvn clean package -pl services -am -DskipTests

echo ""
echo "Build complete!"
echo ""
echo "JAR files created in:"
find services -name "*-container-*.jar" -type f | grep target || echo "  (No JAR files found - check build output for errors)"

