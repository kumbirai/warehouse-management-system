# Utility Scripts

Utility scripts for development and operations.

## Scripts

### Development Scripts

- **`setup-dev-env.sh`** - Complete development environment setup
    - Creates .env file
    - Starts Docker services
    - Builds project

- **`start-services.sh`** - Start infrastructure services (Docker)
    - Starts PostgreSQL, Keycloak, Kafka, Zookeeper, Redis, Kafka UI, pgAdmin, MailHog

- **`stop-services.sh`** - Stop infrastructure services
    - Stops all Docker containers

- **`build-services.sh`** - Build all services
    - Builds common modules first (common-domain, common-messaging, common-application, common-infrastructure,
      common-security, common-keycloak)
    - Builds all service modules (gateway, user, tenant, notification, stock-management, location-management, product,
      picking, returns, reconciliation, integration)

- **`run-tests.sh`** - Run all tests
    - Executes Maven test phase
    - Generates test reports

- **`check-health.sh`** - Check service health
    - Checks Docker services status
    - Verifies PostgreSQL connectivity
    - Verifies Kafka connectivity
    - Verifies Redis connectivity
    - Verifies Keycloak health
    - Verifies Kafka UI accessibility
    - Verifies pgAdmin accessibility
    - Verifies MailHog accessibility

- **`clean-all.sh`** - Clean all build artifacts
    - Stops Docker services
    - Removes Docker volumes
    - Cleans Maven builds

- **`setup-keycloak-client.sh`** - Set up Keycloak client and realm
    - Creates the `wms-realm` realm if it doesn't exist
    - Creates the `wms-api` client with proper configuration (confidential, Direct Access Grants enabled)
    - Generates client secret automatically
    - Creates tenant_id mapper for JWT tokens
    - Outputs the client secret and configuration instructions
    - Configurable via environment variables (KEYCLOAK_SERVER_URL, KEYCLOAK_REALM, etc.)

- **`get-keycloak-client-secret.sh`** - Retrieve Keycloak client secret
    - Retrieves the client secret for the wms-api client from Keycloak
    - Uses Keycloak Admin REST API
    - Outputs the secret and environment variable configuration
    - Configurable via environment variables (KEYCLOAK_SERVER_URL, KEYCLOAK_REALM, etc.)

- **`setup-postgres-databases.sh`** - Set up PostgreSQL databases
    - Creates all required databases for WMS services
    - Supports individual database creation/dropping
    - Lists database status
    - Validates database names
    - Configurable via environment variables (DB_HOST, DB_PORT, DB_USER, DB_PASSWORD)
    - Commands: `create-all`, `drop-all`, `create <db_name>`, `drop <db_name>`, `list`

### Deployment Scripts

- **`deploy-k8s.sh`** - Deploy to Kubernetes
    - Creates namespace
    - Applies ConfigMaps
    - Applies Secrets (with validation)
    - Applies Deployments (currently: stock-management, location-management, product, picking, returns, reconciliation,
      integration)
    - Applies Services
    - Applies Ingress
    - Notes missing deployments (gateway, user, tenant services)

## Usage

All scripts are executable and can be run directly:

```bash
./scripts/setup-dev-env.sh
./scripts/start-services.sh
./scripts/build-services.sh
```

## Prerequisites

- Docker and Docker Compose (for infrastructure scripts)
- Maven (for build scripts)
- kubectl (for Kubernetes deployment script)
- PostgreSQL client (for health check and database setup scripts)
- curl (for health check script)

## Infrastructure Services

The following infrastructure services are managed by Docker Compose:

- **PostgreSQL** (port 5432) - Main application database
- **Keycloak** (port 7080) - Identity and Access Management
- **Kafka** (port 9092) - Message broker
- **Kafka UI** (port 8010) - Kafka management interface
- **Zookeeper** (port 2181) - Kafka coordination
- **Redis** (port 6379) - Caching and rate limiting
- **pgAdmin** (port 5050) - PostgreSQL administration interface
- **MailHog** (ports 1025, 8025) - Email testing (SMTP and UI)

## Application Services

The following application services are built and can be run:

- **Gateway Service** (port 8080) - API Gateway
- **User Service** (port 8088) - User management and IAM
- **Tenant Service** (port 8089) - Tenant lifecycle and configuration
- **Notification Service** (port 8090) - Notification management and delivery
- **Stock Management Service** (port 8081) - Stock consignment and management
- **Location Management Service** (port 8082) - Warehouse location management
- **Product Service** (port 8083) - Product catalog management
- **Picking Service** (port 8084) - Picking operations
- **Returns Service** (port 8085) - Returns processing
- **Reconciliation Service** (port 8086) - Stock reconciliation
- **Integration Service** (port 8087) - External system integration

