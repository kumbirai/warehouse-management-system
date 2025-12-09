#!/bin/bash

echo "Checking service health..."

# Check Docker services
echo ""
echo "=== Docker Services ==="
cd infrastructure/docker
docker-compose -f docker-compose.dev.yml ps

# Check database connectivity
echo ""
echo "=== Database Connectivity ==="
PGPASSWORD=secret psql -h localhost -p 5432 -U postgres -d wms_db -c "SELECT version();" 2>/dev/null && echo "✓ PostgreSQL is accessible" || echo "✗ PostgreSQL is not accessible"

# Check Kafka
echo ""
echo "=== Kafka Connectivity ==="
docker exec wms-kafka-dev kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1 && echo "✓ Kafka is accessible" || echo "✗ Kafka is not accessible"

# Check Redis
echo ""
echo "=== Redis Connectivity ==="
docker exec wms-redis-dev redis-cli ping > /dev/null 2>&1 && echo "✓ Redis is accessible" || echo "✗ Redis is not accessible"

# Check Keycloak
echo ""
echo "=== Keycloak Connectivity ==="
curl -f -s http://localhost:7080/health > /dev/null 2>&1 && echo "✓ Keycloak is accessible" || echo "✗ Keycloak is not accessible"

# Check Kafka UI
echo ""
echo "=== Kafka UI Connectivity ==="
curl -f -s http://localhost:8010 > /dev/null 2>&1 && echo "✓ Kafka UI is accessible" || echo "✗ Kafka UI is not accessible"

# Check pgAdmin
echo ""
echo "=== pgAdmin Connectivity ==="
curl -f -s http://localhost:5050 > /dev/null 2>&1 && echo "✓ pgAdmin is accessible" || echo "✗ pgAdmin is not accessible"

# Check MailHog
echo ""
echo "=== MailHog Connectivity ==="
curl -f -s http://localhost:8025 > /dev/null 2>&1 && echo "✓ MailHog UI is accessible" || echo "✗ MailHog UI is not accessible"

echo ""
echo "Health check complete!"

