#!/bin/bash

set -e

echo "Starting development services..."

cd infrastructure/docker
docker-compose -f docker-compose.dev.yml up -d

echo "Services started. Waiting for health checks..."
sleep 15

echo "Infrastructure Services are ready!"
echo "  PostgreSQL: localhost:5432"
echo "  Keycloak: http://localhost:7080"
echo "  Kafka: localhost:9092"
echo "  Kafka UI: http://localhost:8010"
echo "  Redis: localhost:6379"
echo "  pgAdmin: http://localhost:5050"
echo "  MailHog SMTP: localhost:1025"
echo "  MailHog UI: http://localhost:8025"
echo ""
echo "Note: Application services (Gateway, User, Tenant, etc.) need to be started separately."

