# Keycloak Client Secret Setup Guide

## Overview

The `wms-api` client in Keycloak is configured as a **confidential client**, which requires a client secret for authentication. This guide explains how to retrieve and configure
the client secret.

## Problem

If you see the following error in the logs:

```
WARN  c.c.w.u.m.a.AuthenticationServiceAdapter - Client secret is not configured - this may cause authentication failures for confidential clients
WARN  c.c.w.u.m.a.AuthenticationServiceAdapter - Login failed for user: testuser@cm-sol.co.za - HTTP 401 Unauthorized. Keycloak error: error: invalid_client, description: Invalid client or Invalid client credentials
```

This means the client secret is not configured in the application.

## Solution Options

### Option 1: Set Up Client and Retrieve Secret (Recommended for First-Time Setup)

If the `wms-api` client doesn't exist yet, use the setup script to create it and get the secret:

```bash
./scripts/setup-keycloak-client.sh
```

This script will:

1. Create the `wms-realm` realm if it doesn't exist
2. Create the `wms-api` client with proper configuration
3. Generate the client secret
4. Set up the tenant_id mapper
5. Display the client secret and configuration instructions

**Output Example:**

```
==========================================
Client Secret:
==========================================
B4iTgRx7eWL9NVV2AmnzCZj0Lix5EM6E

==========================================
Environment Variable:
==========================================
export KEYCLOAK_CLIENT_SECRET="B4iTgRx7eWL9NVV2AmnzCZj0Lix5EM6E"
```

### Option 2: Retrieve Client Secret Using Script (If Client Already Exists)

Use the provided script to automatically retrieve the client secret from Keycloak:

```bash
./scripts/get-keycloak-client-secret.sh
```

The script will:

1. Authenticate with Keycloak Admin
2. Find the `wms-api` client in the `wms-realm`
3. Retrieve the client secret
4. Display the secret and environment variable configuration

**Output Example:**

```
==========================================
Client Secret:
==========================================
abc123def456ghi789jkl012mno345pqr678stu901vwx234yz

==========================================
Environment Variable:
==========================================
export KEYCLOAK_CLIENT_SECRET="abc123def456ghi789jkl012mno345pqr678stu901vwx234yz"
```

**Configure the secret:**

1. **Using Environment Variable:**
   ```bash
   export KEYCLOAK_CLIENT_SECRET="your-client-secret-here"
   ```

2. **Using application.yml:**
   ```yaml
   keycloak:
     admin:
       clientSecret: your-client-secret-here
   ```

3. **Using .env file:**
   ```bash
   KEYCLOAK_CLIENT_SECRET=your-client-secret-here
   ```

### Option 3: Retrieve from Keycloak Admin Console

1. Access Keycloak Admin Console: `http://localhost:7080`
2. Login with admin credentials
3. Navigate to: **Realm: `wms-realm`** > **Clients** > **`wms-api`**
4. Go to the **Credentials** tab
5. Copy the **Client Secret** value
6. Configure it in your application (see Option 1, step 2)

### Option 4: Generate New Client Secret

If the client secret is not set or you need to regenerate it:

1. Access Keycloak Admin Console: `http://localhost:7080`
2. Navigate to: **Realm: `wms-realm`** > **Clients** > **`wms-api`**
3. Go to the **Credentials** tab
4. Click **Regenerate Secret**
5. Copy the new secret
6. Configure it in your application (see Option 1, step 2)

## Automatic Retrieval (Optional)

The `ApplicationStartupValidator` can optionally retrieve the client secret automatically if it's not configured. This feature:

- Only attempts retrieval if the secret is not configured
- Logs warnings if retrieval fails (non-blocking)
- Provides helpful instructions in logs

**Note:** This feature requires Keycloak to be accessible and the admin credentials to be configured.

## Script Configuration

The script uses the following environment variables (with defaults):

```bash
KEYCLOAK_SERVER_URL=http://localhost:7080      # Keycloak server URL
KEYCLOAK_ADMIN_REALM=master                    # Admin realm
KEYCLOAK_ADMIN_USERNAME=admin                   # Admin username
KEYCLOAK_ADMIN_PASSWORD=admin                  # Admin password
KEYCLOAK_REALM=wms-realm                       # Target realm
CLIENT_ID=wms-api                               # Client ID
```

You can override these by setting environment variables:

```bash
export KEYCLOAK_SERVER_URL=http://localhost:7080
export KEYCLOAK_ADMIN_USERNAME=admin
export KEYCLOAK_ADMIN_PASSWORD=admin
./scripts/get-keycloak-client-secret.sh
```

## Verification

After configuring the client secret, verify it's working:

1. Check application logs for:
   ```
   ✓ Keycloak client secret: [CONFIGURED]
   ```

2. Attempt a login - you should no longer see the "invalid_client" error

3. Check logs for successful authentication:
   ```
   INFO  c.c.w.u.m.a.AuthenticationServiceAdapter - Login successful for user: testuser@cm-sol.co.za
   ```

## Troubleshooting

### Client Not Found

If the script reports "Client 'wms-api' not found":

1. Verify the client exists in Keycloak:
    - Access Keycloak Admin Console
    - Navigate to Realm: `wms-realm` > Clients
    - Check if `wms-api` client exists

2. If it doesn't exist, create it:
    - Client ID: `wms-api`
    - Client Protocol: `openid-connect`
    - Access Type: `confidential`
    - Direct Access Grants Enabled: `true`
    - Standard Flow Enabled: `true`

### Secret Not Found

If the script reports "Client secret not found":

1. The client might be configured as public (no secret required)
2. The secret might not have been generated
3. Solution: Go to Keycloak Admin Console > Clients > `wms-api` > Credentials > Regenerate Secret

### Authentication Failed

If the script fails to authenticate:

1. Verify Keycloak is running: `curl http://localhost:7080/health`
2. Check admin credentials are correct
3. Verify the admin realm is `master` (default)

## Security Notes

- **Never commit client secrets to version control**
- Use environment variables or secure secret management (e.g., Kubernetes Secrets, HashiCorp Vault)
- Rotate client secrets periodically
- Use different secrets for different environments (dev, staging, production)

## Related Documentation

- [IAM Integration Guide](./IAM_Integration_Guide.md) - Complete Keycloak setup guide
- [Security Architecture Document](../01-architecture/Security_Architecture_Document.md) - Security architecture overview

