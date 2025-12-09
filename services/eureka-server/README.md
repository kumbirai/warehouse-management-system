# Eureka Server

Service Discovery agent for microservices using Netflix Eureka.

## Building the Docker Image

### Prerequisites

1. Build the JAR file first:
   ```bash
   # From project root
   mvn clean package -pl services/eureka-server -am -DskipTests
   ```

2. Build the Docker image:
   ```bash
   # From services/eureka-server directory
   docker build -t wms/eureka-server:latest .
   ```

   Or from project root:
   ```bash
   docker build -f services/eureka-server/Dockerfile -t wms/eureka-server:latest services/eureka-server
   ```

### Using Docker Compose

The service can be deployed using docker-compose:

**Development:**

```bash
cd infrastructure/docker
# Ensure JAR is built first
mvn clean package -pl ../../services/eureka-server -am -DskipTests
docker-compose -f docker-compose.dev.yml up -d eureka-server
```

**Production:**

```bash
cd infrastructure/docker
# Ensure JAR is built first
mvn clean package -pl ../../services/eureka-server -am -DskipTests
docker-compose -f docker-compose.prod.yml up -d eureka-server
```

## Running the Container

```bash
docker run -d \
  --name eureka-server \
  -p 8761:8761 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e EUREKA_INSTANCE_HOSTNAME=eureka-server \
  wms/eureka-server:latest
```

## Accessing the Service

- **Eureka Dashboard:** http://localhost:8761
- **Health Check:** http://localhost:8761/actuator/health

## Configuration

The service can be configured using environment variables:

- `SPRING_PROFILES_ACTIVE`: Spring profile (dev, prod)
- `EUREKA_INSTANCE_HOSTNAME`: Hostname for Eureka instance
- `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE`: Default Eureka service URL

## Ports

- **8761**: Eureka server port

