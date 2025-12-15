package com.ccbsa.wms.gateway.api.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;

import com.ccbsa.wms.gateway.api.dto.LoginRequest;
import com.ccbsa.wms.gateway.api.dto.LoginResponse;
import com.ccbsa.wms.gateway.api.dto.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * Lightweight mock gateway that emulates authentication, tenant, and user
 * endpoints so gateway-api-tests can run without external services.
 */
@Slf4j
public final class MockGatewayServer implements AutoCloseable {

    private static final String DEFAULT_TENANT_ID = "tenant-1";
    private static final String DEFAULT_TENANT_STATUS = "ACTIVE";
    private static final String DEFAULT_USERNAME = "sysadmin";
    private static final String DEFAULT_PASSWORD = "Password123@";
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, TenantRecord> tenants = new ConcurrentHashMap<>();
    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();
    private final Map<String, TokenRecord> accessTokens = new ConcurrentHashMap<>();
    private final Map<String, TokenRecord> refreshTokens = new ConcurrentHashMap<>();

    private DisposableServer server;
    private String baseUrl;

    private MockGatewayServer() {
        seedDefaults();
    }

    private void seedDefaults() {
        TenantRecord defaultTenant = new TenantRecord(
                DEFAULT_TENANT_ID,
                "Default Tenant",
                "tenant@example.com",
                "000-000-0000",
                DEFAULT_TENANT_STATUS);
        tenants.put(defaultTenant.tenantId(), defaultTenant);

        UserRecord sysAdmin = new UserRecord(
                UUID.randomUUID().toString(),
                DEFAULT_USERNAME,
                DEFAULT_PASSWORD,
                DEFAULT_TENANT_ID,
                "sysadmin@example.com",
                "System",
                "Admin",
                "ACTIVE",
                new ArrayList<>(List.of("SYSTEM_ADMIN", "USER")));
        users.put(sysAdmin.userId(), sysAdmin);

        UserRecord adminUser = new UserRecord(
                UUID.randomUUID().toString(),
                "admin",
                "admin",
                DEFAULT_TENANT_ID,
                "admin@example.com",
                "Admin",
                "User",
                "ACTIVE",
                new ArrayList<>(List.of("SYSTEM_ADMIN", "USER")));
        users.put(adminUser.userId(), adminUser);
    }

    public static MockGatewayServer start() {
        MockGatewayServer mockGatewayServer = new MockGatewayServer();
        mockGatewayServer.startServer();
        return mockGatewayServer;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public WebTestClient createWebTestClient() {
        return WebTestClient
                .bindToServer()
                .baseUrl(baseUrl)
                .responseTimeout(RESPONSE_TIMEOUT)
                .build();
    }

    private void startServer() {
        RouterFunction<ServerResponse> router = RouterFunctions.nest(
                RequestPredicates.path("/api/v1"),
                RouterFunctions.route()
                        .POST("/bff/auth/login", this::handleLogin)
                        .POST("/bff/auth/refresh", this::handleRefresh)
                        .GET("/bff/auth/me", this::handleCurrentUser)
                        .POST("/bff/auth/logout", this::handleLogout)
                        .GET("/actuator/health", request -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("status", "UP")))
                        .GET("/actuator/info", request -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("info", "mock-gateway")))
                        .GET("/tenants", this::handleListTenants)
                        .GET("/tenants/{tenantId}", this::handleGetTenant)
                        .POST("/tenants", this::handleCreateTenant)
                        .PUT("/tenants/{tenantId}/activate", this::handleActivateTenant)
                        .PUT("/tenants/{tenantId}/deactivate", this::handleDeactivateTenant)
                        .PUT("/tenants/{tenantId}/suspend", this::handleSuspendTenant)
                        .GET("/users", this::handleListUsers)
                        .GET("/users/{userId}", this::handleGetUser)
                        .POST("/users", this::handleCreateUser)
                        .PUT("/users/{userId}/profile", this::handleUpdateUserProfile)
                        .PUT("/users/{userId}/activate", this::handleActivateUser)
                        .PUT("/users/{userId}/deactivate", this::handleDeactivateUser)
                        .PUT("/users/{userId}/suspend", this::handleSuspendUser)
                        .POST("/users/{userId}/roles", this::handleAssignRole)
                        .GET("/users/{userId}/roles", this::handleGetUserRoles)
                        .build());

        HttpWebHandlerAdapter httpHandler = new HttpWebHandlerAdapter(
                RouterFunctions.toWebHandler(router));

        server = HttpServer.create()
                .port(0)
                .handle(new ReactorHttpHandlerAdapter(httpHandler))
                .bindNow();

        baseUrl = String.format("http://localhost:%d/api/v1", server.port());
        log.info("Mock gateway server started at {}", baseUrl);
    }

    private Mono<ServerResponse> handleLogin(ServerRequest request) {
        return request.bodyToMono(LoginRequest.class)
                .flatMap(loginRequest -> {
                    UserRecord user = users.values().stream()
                            .filter(existing -> existing.username().equals(loginRequest.getUsername()))
                            .findFirst()
                            .orElse(null);
                    if (user == null || !user.password().equals(loginRequest.getPassword())) {
                        return unauthorized();
                    }
                    TokenRecord tokenRecord = issueTokens(user);
                    LoginResponse loginResponse = LoginResponse.builder()
                            .accessToken(tokenRecord.accessToken())
                            .refreshToken(tokenRecord.refreshToken())
                            .tokenType("Bearer")
                            .expiresIn(3600)
                            .userContext(buildUserContext(user))
                            .build();

                    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokenRecord.refreshToken())
                            .path("/api/v1/bff/auth")
                            .httpOnly(true)
                            .secure(true)
                            .sameSite("Strict")
                            .maxAge(Duration.ofDays(1))
                            .build();

                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(refreshCookie)
                            .bodyValue(successResponse(loginResponse));
                });
    }

    private Mono<ServerResponse> handleRefresh(ServerRequest request) {
        String refreshToken = request.cookies().getFirst("refreshToken") != null
                ? request.cookies().getFirst("refreshToken").getValue()
                : null;
        TokenRecord existing = refreshToken != null ? refreshTokens.get(refreshToken) : null;
        UserRecord user = existing != null ? users.get(existing.userId()) : users.values().stream().findFirst().orElse(null);
        if (user == null) {
            return unauthorized();
        }
        TokenRecord refreshed = issueTokens(user);
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(refreshed.accessToken())
                .refreshToken(refreshed.refreshToken())
                .tokenType("Bearer")
                .expiresIn(3600)
                .userContext(buildUserContext(user))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshed.refreshToken())
                .path("/api/v1/bff/auth")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(Duration.ofDays(1))
                .build();

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(refreshCookie)
                .bodyValue(successResponse(loginResponse));
    }

    private Mono<ServerResponse> handleCurrentUser(ServerRequest request) {
        String authorization = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        return authenticateOrUnauthorized(authorization,
                () -> authenticate(authorization)
                        .flatMap(user -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(successResponse(buildUserContext(user)))));
    }

    private Mono<ServerResponse> handleLogout(ServerRequest request) {
        String authorization = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            TokenRecord record = accessTokens.get(tokenValue(authorization));
            if (record != null) {
                accessTokens.remove(record.accessToken());
                refreshTokens.remove(record.refreshToken());
            }
        }
        return ServerResponse.noContent().build();
    }

    private Mono<ServerResponse> handleListTenants(ServerRequest request) {
        return authenticateOrUnauthorized(request.headers().firstHeader(HttpHeaders.AUTHORIZATION),
                () -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(successResponse(new ArrayList<>(tenants.values()))));
    }

    private Mono<ServerResponse> handleGetTenant(ServerRequest request) {
        return authenticateOrUnauthorized(request.headers().firstHeader(HttpHeaders.AUTHORIZATION),
                () -> {
                    String tenantId = request.pathVariable("tenantId");
                    TenantRecord tenant = tenants.get(tenantId);
                    if (tenant == null) {
                        return notFound();
                    }
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(successResponse(tenant));
                });
    }

    private Mono<ServerResponse> handleCreateTenant(ServerRequest request) {
        return authenticateOrUnauthorized(request.headers().firstHeader(HttpHeaders.AUTHORIZATION),
                () -> request.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .flatMap(body -> {
                            String tenantId = valueOrGenerate(body.get("tenantId"));
                            TenantRecord tenant = new TenantRecord(
                                    tenantId,
                                    valueOrDefault(body.get("name"), "Tenant " + tenantId),
                                    valueOrDefault(body.get("emailAddress"), "tenant@" + tenantId + ".com"),
                                    valueOrDefault(body.get("phone"), "000-000-0000"),
                                    "PENDING");
                            tenants.put(tenant.tenantId(), tenant);
                            return ServerResponse.status(HttpStatus.CREATED)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(successResponse(tenant));
                        }));
    }

    private Mono<ServerResponse> handleActivateTenant(ServerRequest request) {
        return updateTenantStatus(request, "ACTIVE");
    }

    private Mono<ServerResponse> handleDeactivateTenant(ServerRequest request) {
        return updateTenantStatus(request, "INACTIVE");
    }

    private Mono<ServerResponse> handleSuspendTenant(ServerRequest request) {
        return updateTenantStatus(request, "SUSPENDED");
    }

    private Mono<ServerResponse> handleListUsers(ServerRequest request) {
        return authenticateOrUnauthorized(request.headers().firstHeader(HttpHeaders.AUTHORIZATION),
                () -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(successResponse(new ArrayList<>(users.values()))));
    }

    private Mono<ServerResponse> handleGetUser(ServerRequest request) {
        return authenticateOrUnauthorized(request.headers().firstHeader(HttpHeaders.AUTHORIZATION),
                () -> {
                    String userId = request.pathVariable("userId");
                    UserRecord user = users.get(userId);
                    if (user == null) {
                        return notFound();
                    }
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(successResponse(user));
                });
    }

    private Mono<ServerResponse> handleCreateUser(ServerRequest request) {
        return authenticateOrUnauthorized(request.headers().firstHeader(HttpHeaders.AUTHORIZATION),
                () -> request.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .flatMap(body -> {
                            String userId = UUID.randomUUID().toString();
                            String username = valueOrDefault(body.get("username"), "user-" + userId);
                            String tenantId = valueOrDefault(body.get("tenantId"), DEFAULT_TENANT_ID);
                            UserRecord user = new UserRecord(
                                    userId,
                                    username,
                                    valueOrDefault(body.get("password"), DEFAULT_PASSWORD),
                                    tenantId,
                                    valueOrDefault(body.get("emailAddress"), username + "@example.com"),
                                    valueOrDefault(body.get("firstName"), "Test"),
                                    valueOrDefault(body.get("lastName"), "User"),
                                    "ACTIVE",
                                    new ArrayList<>(Collections.singletonList("USER")));
                            users.put(user.userId(), user);
                            return ServerResponse.status(HttpStatus.CREATED)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(successResponse(user));
                        }));
    }

    private Mono<ServerResponse> handleUpdateUserProfile(ServerRequest request) {
        return authenticateOrUnauthorized(request.headers().firstHeader(HttpHeaders.AUTHORIZATION),
                () -> {
                    String userId = request.pathVariable("userId");
                    UserRecord existing = users.get(userId);
                    if (existing == null) {
                        return notFound();
                    }
                    return request.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    }).flatMap(body -> {
                        UserRecord updated = existing.withProfile(
                                valueOrDefault(body.get("firstName"), existing.firstName()),
                                valueOrDefault(body.get("lastName"), existing.lastName()),
                                valueOrDefault(body.get("emailAddress"), existing.email()));
                        users.put(userId, updated);
                        return ServerResponse.noContent().build();
                    });
                });
    }

    private Mono<ServerResponse> handleActivateUser(ServerRequest request) {
        return updateUserStatus(request, "ACTIVE");
    }

    private Mono<ServerResponse> handleDeactivateUser(ServerRequest request) {
        return updateUserStatus(request, "INACTIVE");
    }

    private Mono<ServerResponse> handleSuspendUser(ServerRequest request) {
        return updateUserStatus(request, "SUSPENDED");
    }

    private Mono<ServerResponse> handleAssignRole(ServerRequest request) {
        return authenticateOrUnauthorized(request.headers().firstHeader(HttpHeaders.AUTHORIZATION),
                () -> {
                    String userId = request.pathVariable("userId");
                    UserRecord user = users.get(userId);
                    if (user == null) {
                        return notFound();
                    }
                    UserRecord updated = user.withRole("USER");
                    users.put(userId, updated);
                    return ServerResponse.noContent().build();
                });
    }

    private Mono<ServerResponse> handleGetUserRoles(ServerRequest request) {
        return authenticateOrUnauthorized(request.headers().firstHeader(HttpHeaders.AUTHORIZATION),
                () -> {
                    String userId = request.pathVariable("userId");
                    UserRecord user = users.get(userId);
                    if (user == null) {
                        return notFound();
                    }
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(successResponse(user.roles()));
                });
    }

    private Mono<ServerResponse> updateTenantStatus(ServerRequest request, String status) {
        return authenticateOrUnauthorized(request.headers().firstHeader(HttpHeaders.AUTHORIZATION),
                () -> {
                    String tenantId = request.pathVariable("tenantId");
                    TenantRecord tenant = tenants.get(tenantId);
                    if (tenant == null) {
                        return notFound();
                    }
                    tenants.put(tenantId, tenant.withStatus(status));
                    return ServerResponse.noContent().build();
                });
    }

    private Mono<ServerResponse> updateUserStatus(ServerRequest request, String status) {
        return authenticateOrUnauthorized(request.headers().firstHeader(HttpHeaders.AUTHORIZATION),
                () -> {
                    String userId = request.pathVariable("userId");
                    UserRecord user = users.get(userId);
                    if (user == null) {
                        return notFound();
                    }
                    users.put(userId, user.withStatus(status));
                    return ServerResponse.noContent().build();
                });
    }

    private Map<String, Object> successResponse(Object data) {
        return Map.of(
                "success", Boolean.TRUE,
                "data", data);
    }

    private Mono<ServerResponse> unauthorized() {
        return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("success", Boolean.FALSE));
    }

    private Mono<ServerResponse> notFound() {
        return ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("success", Boolean.FALSE));
    }

    private Mono<UserRecord> authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Mono.empty();
        }
        String token = tokenValue(authorizationHeader);
        TokenRecord record = accessTokens.get(token);
        if (record == null) {
            return Mono.empty();
        }
        return Mono.justOrEmpty(users.get(record.userId()));
    }

    private Mono<ServerResponse> authenticateOrUnauthorized(String authorizationHeader,
                                                            SupplierWithMono supplier) {
        return authenticate(authorizationHeader)
                .flatMap(user -> supplier.get())
                .switchIfEmpty(unauthorized());
    }

    private TokenRecord issueTokens(UserRecord user) {
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        TokenRecord record = new TokenRecord(accessToken, refreshToken, user.userId());
        accessTokens.put(accessToken, record);
        refreshTokens.put(refreshToken, record);
        return record;
    }

    private UserContext buildUserContext(UserRecord user) {
        return UserContext.builder()
                .userId(user.userId())
                .username(user.username())
                .tenantId(user.tenantId())
                .roles(user.roles())
                .email(user.email())
                .firstName(user.firstName())
                .lastName(user.lastName())
                .build();
    }

    private String tokenValue(String authorizationHeader) {
        return authorizationHeader.replace("Bearer", "").trim();
    }

    private String valueOrDefault(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private String valueOrGenerate(Object value) {
        return value == null ? UUID.randomUUID().toString() : value.toString();
    }

    @Override
    public void close() {
        if (server != null) {
            server.disposeNow();
        }
    }

    @FunctionalInterface
    private interface SupplierWithMono {
        Mono<ServerResponse> get();
    }

    private record TenantRecord(String tenantId,
                                String name,
                                String emailAddress,
                                String phone,
                                String status) {
        TenantRecord withStatus(String newStatus) {
            return new TenantRecord(tenantId, name, emailAddress, phone, newStatus);
        }
    }

    private record UserRecord(String userId,
                              String username,
                              String password,
                              String tenantId,
                              String email,
                              String firstName,
                              String lastName,
                              String status,
                              List<String> roles) {
        UserRecord withStatus(String newStatus) {
            return new UserRecord(userId, username, password, tenantId, email, firstName, lastName, newStatus, roles);
        }

        UserRecord withProfile(String newFirstName, String newLastName, String newEmail) {
            return new UserRecord(userId, username, password, tenantId, newEmail, newFirstName, newLastName, status, roles);
        }

        UserRecord withRole(String role) {
            List<String> updatedRoles = new ArrayList<>(roles);
            if (!updatedRoles.contains(role)) {
                updatedRoles.add(role);
            }
            return new UserRecord(userId, username, password, tenantId, email, firstName, lastName, status, updatedRoles);
        }
    }

    private record TokenRecord(String accessToken, String refreshToken, String userId) {
    }
}


