#!/bin/bash

set -e

echo "Setting up development environment..."

# Check prerequisites
echo "Checking prerequisites..."
command -v java >/dev/null 2>&1 || { echo "Java is required but not installed. Aborting." >&2; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "Maven is required but not installed. Aborting." >&2; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "Docker is required but not installed. Aborting." >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "Docker Compose is required but not installed. Aborting." >&2; exit 1; }

# Create .env file from example (if it exists)
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        cp .env.example .env
        echo "Created .env file from .env.example. Please update with your values."
    else
        echo "Warning: .env.example not found. Creating empty .env file."
        touch .env
        echo "# Development Environment Configuration" >> .env
        echo "# Add your configuration here" >> .env
    fi
else
    echo ".env file already exists. Skipping creation."
fi

# Start Docker services
echo "Starting Docker services..."
cd infrastructure/docker
docker-compose -f docker-compose.dev.yml up -d

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 15

# Check service health
echo "Checking service health..."
docker-compose -f docker-compose.dev.yml ps

# Return to project root
cd ../..

# Build project
echo "Building project..."
mvn clean install -DskipTests

echo "Development environment setup complete!"
echo ""
echo "Infrastructure Services:"
echo "  PostgreSQL: localhost:5432"
echo "  Keycloak: http://localhost:7080"
echo "  Kafka: localhost:9092"
echo "  Kafka UI: http://localhost:8010"
echo "  Redis: localhost:6379"
echo "  pgAdmin: http://localhost:5050"
echo "  MailHog SMTP: localhost:1025"
echo "  MailHog UI: http://localhost:8025"
echo ""
echo "Application Services:"
echo "  Gateway Service: http://localhost:8080"
echo "  Stock Management: http://localhost:8081"
echo "  Location Management: http://localhost:8082"
echo "  Product Service: http://localhost:8083"
echo "  Picking Service: http://localhost:8084"
echo "  Returns Service: http://localhost:8085"
echo "  Reconciliation Service: http://localhost:8086"
echo "  Integration Service: http://localhost:8087"
echo "  User Service: http://localhost:8088"
echo "  Tenant Service: http://localhost:8089"
echo "  Notification Service: http://localhost:8090"
echo ""
echo "Next steps:"
echo "  1. Update .env file with your configuration"
echo "  2. Run database migrations: mvn flyway:migrate -pl services/*-service"
echo "  3. Start services individually or use start-services.sh"

