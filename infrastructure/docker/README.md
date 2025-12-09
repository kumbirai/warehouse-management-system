# Docker Configuration

This directory contains Docker Compose configurations for local development and production environments.

## Files

- `docker-compose.dev.yml` - Development environment configuration
- `docker-compose.prod.yml` - Production environment configuration

## Development Environment

### Services

The development environment includes:

- **PostgreSQL** (port 5432) - Main application database
- **PostgreSQL Keycloak** (port 5433) - Keycloak database
- **Zookeeper** (port 2181) - Kafka coordination
- **Kafka** (port 9092) - Message broker
- **Redis** (port 6379) - Rate limiting and caching
- **Keycloak** (port 8080) - Identity and Access Management

### Starting Services

```bash
cd infrastructure/docker
docker-compose -f docker-compose.dev.yml up -d
```

### Stopping Services

```bash
cd infrastructure/docker
docker-compose -f docker-compose.dev.yml down
```

### Viewing Logs

```bash
cd infrastructure/docker
docker-compose -f docker-compose.dev.yml logs -f [service-name]
```

### Keycloak Setup

After starting Keycloak:

1. Access Admin Console: http://localhost:7080
2. Login with:
    - Username: `admin`
    - Password: `admin`
3. Create realm: `wms-realm`
4. Configure clients and users (see [IAM Integration Guide](../../documentation/01-project-planning/IAM_Integration_Guide.md))

### Service URLs

- PostgreSQL: `localhost:5432`
- Keycloak: `http://localhost:7080`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Gateway Service: `http://localhost:8080`
- Stock Management Service: `http://localhost:8081`
- Location Management Service: `http://localhost:8082`
- Product Service: `http://localhost:8083`
- Picking Service: `http://localhost:8084`
- Returns Service: `http://localhost:8085`
- Reconciliation Service: `http://localhost:8086`
- Integration Service: `http://localhost:8087`
- User Service: `http://localhost:8088`

## Production Environment

Production configuration uses environment variables for sensitive data and includes additional services for monitoring and logging.

