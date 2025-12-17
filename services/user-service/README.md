# User Service - BFF Implementation

## Overview

The User Service implements the **Backend for Frontend (BFF)** pattern to mask Keycloak IAM complexity from frontend
clients. This service provides authentication endpoints that
abstract away Keycloak-specific details.

## Architecture

### BFF Pattern

The BFF pattern is implemented to:

- **Hide IAM complexity**: Frontend doesn't need to know about Keycloak realms, clients, or token formats
- **Centralize authentication logic**: All authentication logic is in one place
- **Simplify frontend integration**: Frontend only needs to call simple REST endpoints
- **Enable future changes**: Can switch IAM providers without frontend changes

### Endpoints

#### Public Endpoints (No Authentication Required)

- `POST /api/v1/bff/auth/login` - User login
- `POST /api/v1/bff/auth/refresh` - Token refresh

#### Protected Endpoints (JWT Required)

- `GET /api/v1/bff/auth/me` - Get current user context
- `POST /api/v1/bff/auth/logout` - Logout (client-side token removal)

## Configuration

### Required Environment Variables

```bash
# Keycloak Configuration
KEYCLOAK_SERVER_URL=http://localhost:7080
KEYCLOAK_DEFAULT_REALM=wms-realm
KEYCLOAK_CLIENT_SECRET=your-client-secret-here

# Keycloak Admin (for user management operations)
KEYCLOAK_ADMIN_REALM=master
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_ADMIN_CLIENT_ID=admin-cli
```

### Application Configuration

See `user-container/src/main/resources/application.yml` for full configuration.

## Keycloak Setup

### Client Configuration

1. **Create Confidential Client:**
    - Client ID: `wms-api`
    - Access Type: `confidential`
    - Direct Access Grants: `enabled`
    - Generate client secret and configure in `KEYCLOAK_CLIENT_SECRET`

2. **Token Configuration:**
    - Access Token Lifespan: 1 hour
    - Refresh Token Lifespan: 24 hours

### User Attributes

Users should have the following attributes:

- `tenant_id` - Tenant identifier (mapped to JWT claim)

### Roles

- `SYSTEM_ADMIN` - System administrators
- `USER` - Regular tenant users

## Security

### Security Headers

The service adds the following security headers to all responses:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `Content-Security-Policy`
- `Referrer-Policy: strict-origin-when-cross-origin`

### Token Security

- Access tokens are returned to frontend (stored in memory/localStorage)
- Refresh tokens are used for token renewal
- Tokens are validated on every protected endpoint request
- Token expiration is handled automatically

## Error Handling

### Error Response Format

All errors follow the standardized `ApiResponse` format:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "timestamp": "2025-01-15T10:30:00Z",
    "path": "/api/v1/bff/auth/login"
  }
}
```

### Common Error Codes

- `AUTHENTICATION_FAILED` - Invalid username or password
- `INVALID_REFRESH_TOKEN` - Invalid or expired refresh token
- `VALIDATION_ERROR` - Request validation failed
- `INTERNAL_ERROR` - Internal server error

## Logging

### Log Levels

- `INFO` - General application flow
- `DEBUG` - Detailed BFF authentication flow
- `WARN` - Authentication failures, validation errors
- `ERROR` - Unexpected errors, network failures

### Log Files

Logs are written to:

- Console (stdout)
- File: `logs/user-service.log` (rotated, max 10MB, 30 days retention)

## Testing

### Manual Testing

```bash
# Login
curl -X POST http://localhost:8088/api/v1/bff/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user@ldp-123.com",
    "password": "password"
  }'

# Refresh Token
curl -X POST http://localhost:8088/api/v1/bff/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'

# Get Current User (requires JWT)
curl -X GET http://localhost:8088/api/v1/bff/auth/me \
  -H "Authorization: Bearer your-access-token"
```

## Production Considerations

1. **Client Secret**: Store in secure secret management (e.g., Kubernetes secrets, HashiCorp Vault)
2. **HTTPS**: Always use HTTPS in production
3. **Rate Limiting**: Consider adding rate limiting to login/refresh endpoints
4. **Monitoring**: Monitor authentication success/failure rates
5. **Token Blacklisting**: Consider implementing token blacklisting for logout (requires token store)
6. **CORS**: Configure CORS appropriately for your frontend domain

## Troubleshooting

### Common Issues

1. **"Keycloak server URL is not configured"**
    - Set `KEYCLOAK_SERVER_URL` environment variable

2. **"Invalid username or password"**
    - Verify user exists in Keycloak
    - Verify user is enabled
    - Check Keycloak client secret configuration

3. **"Unable to connect to authentication service"**
    - Verify Keycloak is running and accessible
    - Check network connectivity
    - Verify firewall rules

4. **"Invalid JWT token format"**
    - Token may be corrupted or malformed
    - Check token extraction logic

