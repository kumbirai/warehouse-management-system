#!/bin/bash

set -e

echo "Running tests..."

# Run all tests
mvn test

# Generate test reports
echo "Test reports generated in:"
find . -name "surefire-reports" -type d | head -5

echo ""
echo "Test execution complete!"

