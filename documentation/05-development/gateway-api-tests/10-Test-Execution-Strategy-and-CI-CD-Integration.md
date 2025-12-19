# Test Execution Strategy and CI/CD Integration Plan

## Overview

This document provides comprehensive strategies for test execution, CI/CD integration, test reporting, and continuous quality assurance for the gateway API integration tests.

---

## 1. Test Execution Strategy

### Test Organization

#### Test Categories

1. **Smoke Tests**: Critical path tests (login, tenant creation, user creation)
2. **Regression Tests**: Full test suite for all services
3. **Integration Tests**: End-to-end workflows
4. **Performance Tests**: Load testing and benchmarking
5. **Security Tests**: Authentication, authorization, tenant isolation

#### Test Execution Order

```
Phase 1: Infrastructure Setup
├── Start gateway service
├── Start all microservices (Eureka discovery)
├── Start Keycloak
├── Start PostgreSQL
└── Start Kafka

Phase 2: Authentication Tests
├── Login/logout tests
├── Token refresh tests
└── Cookie management tests

Phase 3: SYSTEM_ADMIN Tests
├── Tenant management tests
├── User creation tests (cross-tenant)
└── Role assignment tests

Phase 4: TENANT_ADMIN Setup
├── Wait for TENANT_ADMIN credentials
└── Login as TENANT_ADMIN

Phase 5: TENANT_ADMIN Tests
├── User management tests (own tenant)
├── Product management tests
├── Location management tests
├── Stock management tests
├── Picking service tests
├── Returns service tests
└── Reconciliation service tests

Phase 6: Authorization Tests
├── Role-based access control tests
└── Tenant isolation tests

Phase 7: Integration Tests
├── End-to-end workflows
└── Cross-service integration
```

---

## 2. Maven Configuration

### pom.xml Test Configuration

```xml
<project>
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Test execution properties -->
        <skipTests>false</skipTests>
        <skipITs>false</skipITs>

        <!-- Test environment -->
        <gateway.base.url>http://localhost:8080</gateway.base.url>
        <test.system.admin.username>sysadmin</test.system.admin.username>
        <test.system.admin.password>Password123@</test.system.admin.password>
    </properties>

    <build>
        <plugins>
            <!-- Surefire Plugin for Unit Tests -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.0.0</version>
                <configuration>
                    <skipTests>${skipTests}</skipTests>
                    <includes>
                        <include>**/*Test.java</include>
                    </includes>
                    <excludes>
                        <exclude>**/*IntegrationTest.java</exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- Failsafe Plugin for Integration Tests -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>3.0.0</version>
                <configuration>
                    <skipITs>${skipITs}</skipITs>
                    <includes>
                        <include>**/*IntegrationTest.java</include>
                        <include>**/AuthenticationTest.java</include>
                        <include>**/TenantManagementTest.java</include>
                        <include>**/UserManagementTest.java</include>
                        <include>**/ProductManagementTest.java</include>
                        <include>**/LocationManagementTest.java</include>
                        <include>**/StockManagementTest.java</include>
                        <include>**/PickingServiceTest.java</include>
                        <include>**/ReturnsServiceTest.java</include>
                        <include>**/ReconciliationServiceTest.java</include>
                    </includes>
                    <systemPropertyVariables>
                        <gateway.base.url>${gateway.base.url}</gateway.base.url>
                        <test.system.admin.username>${test.system.admin.username}</test.system.admin.username>
                        <test.system.admin.password>${test.system.admin.password}</test.system.admin.password>
                    </systemPropertyVariables>
                    <argLine>
                        -Xmx2048m
                        -Dfile.encoding=UTF-8
                    </argLine>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- Surefire Report Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-report-plugin</artifactId>
                <version>3.0.0</version>
                <executions>
                    <execution>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 3. Test Execution Profiles

### Maven Profiles

```xml
<profiles>
    <!-- Smoke Tests Profile -->
    <profile>
        <id>smoke</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <configuration>
                        <includes>
                            <include>**/AuthenticationTest.java</include>
                            <include>**/TenantManagementTest.java#testCreateTenant_Success</include>
                            <include>**/UserManagementTest.java#testCreateUser_Success_SystemAdmin</include>
                        </includes>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>

    <!-- Full Regression Profile -->
    <profile>
        <id>regression</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <configuration>
                        <includes>
                            <include>**/*Test.java</include>
                        </includes>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>

    <!-- CI/CD Profile -->
    <profile>
        <id>ci</id>
        <properties>
            <gateway.base.url>http://gateway:8080</gateway.base.url>
        </properties>
    </profile>

    <!-- Local Development Profile -->
    <profile>
        <id>local</id>
        <properties>
            <gateway.base.url>http://localhost:8080</gateway.base.url>
        </properties>
    </profile>
</profiles>
```

---

## 4. Test Execution Commands

### Maven Commands

```bash
# Run all integration tests
mvn clean verify

# Run smoke tests only
mvn clean verify -Psmoke

# Run specific test class
mvn clean verify -Dit.test=AuthenticationTest

# Run specific test method
mvn clean verify -Dit.test=AuthenticationTest#testLogin_Success

# Skip integration tests
mvn clean install -DskipITs=true

# Run tests with custom gateway URL
mvn clean verify -Dgateway.base.url=http://192.168.1.100:8080

# Run tests with custom credentials
mvn clean verify \
  -Dtest.system.admin.username=admin \
  -Dtest.system.admin.password=MyPassword123@

# Run tests in parallel (use with caution)
mvn clean verify -DforkCount=2 -DreuseForks=true

# Generate test reports
mvn surefire-report:report
```

---

## 5. CI/CD Integration

### GitHub Actions Workflow

Create `.github/workflows/gateway-api-tests.yml`:

```yaml
name: Gateway API Integration Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
  schedule:
    - cron: '0 2 * * *'  # Run nightly at 2 AM

jobs:
  integration-tests:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_USER: wms_user
          POSTGRES_PASSWORD: wms_password
          POSTGRES_DB: wms_db
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      keycloak:
        image: quay.io/keycloak/keycloak:23.0
        env:
          KEYCLOAK_ADMIN: admin
          KEYCLOAK_ADMIN_PASSWORD: admin
          KC_DB: postgres
          KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
          KC_DB_USERNAME: wms_user
          KC_DB_PASSWORD: wms_password
        ports:
          - 8180:8080
        options: >-
          --health-cmd "curl -f http://localhost:8080/health/ready || exit 1"
          --health-interval 30s
          --health-timeout 10s
          --health-retries 5

      kafka:
        image: confluentinc/cp-kafka:7.5.0
        env:
          KAFKA_BROKER_ID: 1
          KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
        ports:
          - 9092:9092

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Start Eureka Server
        run: |
          cd services/eureka-server
          mvn clean install -DskipTests
          nohup mvn spring-boot:run &
          sleep 30

      - name: Start Gateway Service
        run: |
          cd services/gateway-service
          mvn clean install -DskipTests
          nohup mvn spring-boot:run &
          sleep 30

      - name: Start Microservices
        run: |
          # Start all microservices in background
          cd services/tenant-service && nohup mvn spring-boot:run &
          cd services/user-service && nohup mvn spring-boot:run &
          cd services/product-service && nohup mvn spring-boot:run &
          cd services/location-management-service && nohup mvn spring-boot:run &
          cd services/stock-management-service && nohup mvn spring-boot:run &
          cd services/picking-service && nohup mvn spring-boot:run &
          cd services/returns-service && nohup mvn spring-boot:run &
          cd services/reconciliation-service && nohup mvn spring-boot:run &
          sleep 60

      - name: Wait for services to be ready
        run: |
          timeout 300 bash -c 'until curl -f http://localhost:8080/actuator/health; do sleep 5; done'

      - name: Run Integration Tests
        run: |
          cd gateway-api-tests
          mvn clean verify -Pci \
            -Dgateway.base.url=http://localhost:8080 \
            -Dtest.system.admin.username=${{ secrets.TEST_SYSTEM_ADMIN_USERNAME }} \
            -Dtest.system.admin.password=${{ secrets.TEST_SYSTEM_ADMIN_PASSWORD }}

      - name: Generate Test Report
        if: always()
        run: |
          cd gateway-api-tests
          mvn surefire-report:report

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: gateway-api-tests/target/surefire-reports/

      - name: Upload Test Reports
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: gateway-api-tests/target/site/

      - name: Publish Test Results
        if: always()
        uses: EnricoMi/publish-unit-test-result-action@v2
        with:
          files: gateway-api-tests/target/surefire-reports/TEST-*.xml

      - name: Collect Service Logs
        if: failure()
        run: |
          mkdir -p logs
          docker logs gateway-service > logs/gateway.log 2>&1 || true
          docker logs tenant-service > logs/tenant.log 2>&1 || true
          docker logs user-service > logs/user.log 2>&1 || true

      - name: Upload Service Logs
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: service-logs
          path: logs/
```

---

## 6. Docker Compose for Local Testing

Create `docker-compose.test.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: wms_user
      POSTGRES_PASSWORD: wms_password
      POSTGRES_DB: wms_db
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U wms_user"]
      interval: 10s
      timeout: 5s
      retries: 5

  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: wms_user
      KC_DB_PASSWORD: wms_password
    ports:
      - "8180:8080"
    depends_on:
      - postgres
    command: start-dev

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  gateway:
    build:
      context: ./services/gateway-service
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: test
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka:8761/eureka/
    depends_on:
      - postgres
      - keycloak
      - kafka
      - redis
```

### Run Tests with Docker Compose

```bash
# Start services
docker-compose -f docker-compose.test.yml up -d

# Wait for services to be ready
sleep 60

# Run tests
cd gateway-api-tests
mvn clean verify -Plocal

# Stop services
docker-compose -f docker-compose.test.yml down
```

---

## 7. Test Reporting

### JUnit HTML Report

Configure Maven Surefire Report Plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-report-plugin</artifactId>
    <version>3.0.0</version>
    <configuration>
        <outputDirectory>${project.build.directory}/test-reports</outputDirectory>
        <outputName>integration-test-report</outputName>
    </configuration>
</plugin>
```

### Allure Report Integration

Add Allure to `pom.xml`:

```xml
<dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-junit5</artifactId>
    <version>2.24.0</version>
    <scope>test</scope>
</dependency>

<plugin>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-maven</artifactId>
    <version>2.12.0</version>
    <configuration>
        <reportVersion>2.24.0</reportVersion>
    </configuration>
</plugin>
```

Generate Allure report:

```bash
# Run tests
mvn clean verify

# Generate Allure report
mvn allure:report

# Open report
mvn allure:serve
```

---

## 8. Test Data Management

### Test Data Cleanup Strategy

#### Option 1: No Cleanup (Manual Inspection)

- Leave test data in database
- Useful for debugging failed tests
- Requires manual cleanup

#### Option 2: Cleanup After Each Test

```java
@AfterEach
public void cleanupTestData() {
    // Delete created test data
    if (createdTenantId != null) {
        deleteTenant(createdTenantId);
    }
}
```

#### Option 3: Cleanup After All Tests

```java
@AfterAll
public static void cleanupAllTestData() {
    // Bulk delete test data
    deleteTestTenants();
    deleteTestUsers();
}
```

#### Option 4: Database Reset (Test Containers)

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("wms_test_db")
        .withUsername("test")
        .withPassword("test");
```

---

## 9. Performance Testing

### JMeter Integration

Create `gateway-api-load-test.jmx`:

```xml
<!-- JMeter test plan for load testing -->
<jmeterTestPlan>
    <hashTree>
        <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Gateway API Load Test">
            <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Users">
                <stringProp name="ThreadGroup.num_threads">100</stringProp>
                <stringProp name="ThreadGroup.ramp_time">60</stringProp>
                <stringProp name="ThreadGroup.duration">300</stringProp>
            </ThreadGroup>
        </TestPlan>
    </hashTree>
</jmeterTestPlan>
```

Run JMeter test:

```bash
jmeter -n -t gateway-api-load-test.jmx -l results.jtl -e -o report/
```

---

## 10. Continuous Quality Metrics

### SonarQube Integration

Add to `pom.xml`:

```xml
<properties>
    <sonar.host.url>http://localhost:9000</sonar.host.url>
    <sonar.projectKey>gateway-api-tests</sonar.projectKey>
</properties>

<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>
```

Run SonarQube analysis:

```bash
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<token>
```

---

## Testing Checklist

- [ ] Maven Failsafe configured for integration tests
- [ ] Test profiles created (smoke, regression, ci)
- [ ] GitHub Actions workflow configured
- [ ] Docker Compose for local testing
- [ ] Test reporting configured (JUnit, Allure)
- [ ] Test data cleanup strategy defined
- [ ] Performance testing plan created
- [ ] SonarQube integration configured
- [ ] CI/CD pipeline triggers on PR and push
- [ ] Test results published to GitHub Actions

---

## Next Steps

1. **Configure Maven plugins** (Failsafe, Surefire Report)
2. **Create test execution profiles** (smoke, regression, ci)
3. **Set up GitHub Actions workflow** with service containers
4. **Configure Docker Compose** for local testing
5. **Integrate Allure** for rich test reporting
6. **Set up test data cleanup** strategy
7. **Configure SonarQube** for code quality metrics
8. **Document test execution procedures** for team

---

## Notes

- **Parallel Execution**: Use with caution due to shared resources (database, Kafka)
- **Test Isolation**: Ensure tests are independent and can run in any order
- **Environment Variables**: Use environment variables for sensitive credentials
- **Service Health Checks**: Always wait for services to be ready before running tests
- **Test Reports**: Archive test reports for historical analysis
- **Nightly Builds**: Schedule nightly regression tests to catch regressions early
