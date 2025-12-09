#!/bin/bash

set -e

echo "Cleaning all build artifacts and containers..."

# Stop Docker services
echo "Stopping Docker services..."
cd infrastructure/docker
docker-compose -f docker-compose.dev.yml down -v

# Clean Maven builds
echo "Cleaning Maven builds..."
cd ../..
mvn clean

# Remove Docker volumes
echo "Removing Docker volumes..."
docker volume prune -f

echo "Cleanup complete!"

