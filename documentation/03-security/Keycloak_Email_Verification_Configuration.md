# Keycloak Email Verification and Password Reset Configuration

**Date:** 2025-01  
**Status:** Configuration Guide  
**Related Documents:**
- [IAM Integration Guide](IAM_Integration_Guide.md)
- [Authentication Implementation Complete](Authentication_Implementation_Complete.md)

---

## Overview

This document describes the Keycloak configuration required for email verification and password reset functionality in the user registration and first login experience.

---

## Required Configuration

### 1. SMTP Email Configuration

**Location:** Keycloak Admin Console → Realm Settings → Email

**Required Settings:**

- **Host:** SMTP server hostname (e.g., `smtp.gmail.com`, `smtp.office365.com`)
- **Port:** SMTP port (typically `587` for TLS or `465` for SSL)
- **From:** Sender email address (e.g., `noreply@wms.local`)
- **From Display Name:** Display name for sender (e.g., `WMS Team`)
- **Reply To:** Reply-to email address (optional)
- **Enable StartTLS:** `ON` (for port 587) or `OFF` (for port 465 with SSL)
- **Enable Authentication:** `ON` (if SMTP requires authentication)
- **Username:** SMTP username (if authentication required)
- **Password:** SMTP password (if authentication required)

**Test Connection:** Use the "Test connection" button to verify SMTP settings.

**Example Configuration (Production):**

```
Host: smtp.gmail.com
Port: 587
From: noreply@wms.local
From Display Name: WMS Team
Enable StartTLS: ON
Enable Authentication: ON
Username: your-smtp-username
Password: your-smtp-password
```

#### 1.1 MailHog Setup for Local Development

**MailHog** is an email testing tool that captures all emails sent by your application without actually sending them. It's perfect for local development and testing.

**MailHog is already configured in the project's Docker Compose setup.**

**Access MailHog:**

1. **Start MailHog** (if not already running):
   ```bash
   cd infrastructure/docker
   docker-compose -f docker-compose.dev.yml up -d mailhog
   ```

   Or start all services:
   ```bash
   ./scripts/start-services.sh
   ```

2. **Access MailHog Web UI:**
   - Open your browser and navigate to: `http://localhost:8025`
   - All emails sent by Keycloak will appear in the MailHog inbox
   - You can view email content, HTML rendering, and click links directly

3. **MailHog SMTP Configuration:**
   - **SMTP Host:** `localhost` (or `mailhog` if Keycloak is in Docker network)
   - **SMTP Port:** `1025`
   - **No authentication required**
   - **No TLS/SSL required**

**Keycloak Configuration for MailHog:**

1. Log in to Keycloak Admin Console: `http://localhost:7080`
2. Select your realm (e.g., `wms-realm`)
3. Navigate to **Realm Settings** → **Email** tab
4. Configure as follows:

   ```
   Host: localhost
   Port: 1025
   From: noreply@wms.local
   From Display Name: WMS Team
   Enable StartTLS: OFF
   Enable Authentication: OFF
   Username: (leave empty)
   Password: (leave empty)
   ```

5. Click **Test connection** to verify (should succeed immediately)
6. Click **Save**

**Important Notes:**

- **Docker Network:** 
  - If Keycloak is running **outside Docker** (standalone), use `localhost` as the hostname
  - If Keycloak is running **inside Docker** (via docker-compose), use `mailhog` as the hostname (the Docker service name)
  - To check if Keycloak is in Docker: `docker ps | grep keycloak`
- **Email Links:** Links in MailHog emails are fully functional - you can click verification and password reset links directly
- **Email Persistence:** MailHog stores emails in memory - restarting MailHog will clear the inbox
- **Production:** Never use MailHog in production - use a real SMTP server
- **Port Conflicts:** If ports 1025 or 8025 are already in use, modify `docker-compose.dev.yml` to use different ports

**Verifying MailHog is Working:**

1. Create a test user via User Service API
2. Check MailHog UI at `http://localhost:8025`
3. You should see the verification email appear in the inbox
4. Click on the email to view its contents
5. Click the verification link in the email - it should redirect to your frontend

**Troubleshooting MailHog:**

- **MailHog not receiving emails:**
  - Verify MailHog is running: `docker ps | grep mailhog`
  - Check Keycloak host setting:
    - If Keycloak is **outside Docker**: use `localhost`
    - If Keycloak is **inside Docker**: use `mailhog` (Docker service name)
  - Check Keycloak logs for SMTP connection errors
  - Verify port 1025 is not blocked by firewall
  - Test SMTP connection in Keycloak Admin Console → Realm Settings → Email → Test connection

- **Cannot access MailHog UI:**
  - Verify MailHog container is running: `docker ps | grep mailhog`
  - Check port 8025 is not in use by another service: `lsof -i :8025` or `netstat -an | grep 8025`
  - Try accessing via `http://127.0.0.1:8025`
  - Restart MailHog: `docker restart wms-mailhog-dev`

- **Keycloak cannot connect to MailHog:**
  - If Keycloak is in Docker, ensure both are on the same Docker network (`wms-dev-network`)
  - Check Docker network: `docker network inspect wms-dev-network`
  - Verify MailHog service name matches: `mailhog` (as defined in docker-compose.dev.yml)

### 2. Email Verification Settings

**Location:** Keycloak Admin Console → Realm Settings → Login

**Required Settings:**

- **Email Verification:** `ON` (enabled)
- **Email Verification Required:** `ON` (users must verify email before login)

**Action Token Expiration:**

- **Email Verification Token Lifespan:** `86400` seconds (24 hours)
  - Location: Realm Settings → Tokens → Email Verification Token Lifespan

### 3. Password Reset Settings

**Location:** Keycloak Admin Console → Realm Settings → Login

**Required Settings:**

- **Reset Password:** `ON` (enabled)
- **Forgot Password:** `ON` (enabled)

**Action Token Expiration:**

- **Reset Password Token Lifespan:** `86400` seconds (24 hours)
  - Location: Realm Settings → Tokens → Reset Password Token Lifespan

### 4. Redirect URI Configuration

**Location:** Keycloak Admin Console → Clients → `wms-api` → Settings

**Required Settings:**

- **Valid Redirect URIs:** Add frontend verification and password setup URLs:
  - `http://localhost:3000/verify-email*` (development)
  - `https://your-domain.com/verify-email*` (production)
  - `http://localhost:3000/setup-password*` (development)
  - `https://your-domain.com/setup-password*` (production)

**Note:** Use wildcard `*` to allow query parameters (tokens, keys, etc.)

### 5. Email Templates (Optional)

**Location:** Keycloak Admin Console → Realm Settings → Email

Keycloak provides default email templates for:
- Email verification
- Password reset

**Customization (Optional):**

To customize email templates:
1. Navigate to Realm Settings → Email
2. Select the template to customize (e.g., "Email Verification", "Password Reset")
3. Edit the template using Freemarker syntax
4. Save changes

**Template Variables Available:**
- `${link}` - Verification/reset link
- `${linkExpiration}` - Link expiration time
- `${user}` - User object with properties (username, email, firstName, lastName)
- `${realmName}` - Realm name

### 6. Action Token Expiration Configuration

**Location:** Keycloak Admin Console → Realm Settings → Tokens

**Recommended Settings:**

- **Email Verification Token Lifespan:** `86400` seconds (24 hours)
- **Reset Password Token Lifespan:** `86400` seconds (24 hours)
- **Action Token Generated By User Lifespan:** `300` seconds (5 minutes)

**Security Considerations:**

- Shorter token lifespans improve security but may frustrate users
- 24 hours is a good balance between security and usability
- Tokens are single-use and cannot be reused after expiration

---

## Configuration Steps

### Step 1: Configure SMTP

**For Local Development (MailHog):**

1. Ensure MailHog is running: `docker-compose -f infrastructure/docker/docker-compose.dev.yml up -d mailhog`
2. Log in to Keycloak Admin Console: `http://localhost:7080`
3. Select your realm (e.g., `wms-realm`)
4. Navigate to **Realm Settings** → **Email** tab
5. Configure MailHog settings:
   - Host: `localhost` (or `mailhog` if Keycloak is in Docker network)
   - Port: `1025`
   - From: `noreply@wms.local`
   - Enable StartTLS: `OFF`
   - Enable Authentication: `OFF`
6. Click **Test connection** to verify
7. Click **Save**
8. Access MailHog UI at `http://localhost:8025` to view emails

**For Production:**

1. Log in to Keycloak Admin Console
2. Select your realm (e.g., `wms-realm`)
3. Navigate to **Realm Settings** → **Email** tab
4. Enter production SMTP configuration (Gmail, Office365, SendGrid, etc.)
5. Click **Test connection** to verify
6. Click **Save**

### Step 2: Enable Email Verification

1. Navigate to **Realm Settings** → **Login** tab
2. Enable **Email Verification**
3. Enable **Email Verification Required** (optional, but recommended)
4. Click **Save**

### Step 3: Configure Token Expiration

1. Navigate to **Realm Settings** → **Tokens** tab
2. Set **Email Verification Token Lifespan** to `86400` (24 hours)
3. Set **Reset Password Token Lifespan** to `86400` (24 hours)
4. Click **Save**

### Step 4: Configure Redirect URIs

1. Navigate to **Clients** → Select `wms-api` client
2. Go to **Settings** tab
3. Add redirect URIs:
   - `http://localhost:3000/verify-email*`
   - `http://localhost:3000/setup-password*`
   - (Add production URLs as needed)
4. Click **Save**

### Step 5: Test Configuration

**For Local Development (MailHog):**

1. Ensure MailHog is running and accessible at `http://localhost:8025`
2. Create a test user via User Service API
3. Open MailHog UI (`http://localhost:8025`) - you should see the verification email
4. Click on the email in MailHog to view its contents
5. Click the verification link in the email - it should redirect to your frontend verification page
6. Test password setup flow by clicking the password reset link
7. Verify all links work correctly

**For Production:**

1. Create a test user via User Service API
2. Check the user's email inbox for verification email
3. Click verification link in email
4. Verify redirect to frontend verification page
5. Test password setup flow

---

## Environment Variables

The following environment variables should be configured:

**User Service (`services/user-service/user-container/src/main/resources/application.yml`):**

```yaml
frontend:
  base-url: ${FRONTEND_BASE_URL:http://localhost:3000}
```

**Keycloak Configuration:**

```yaml
keycloak:
  admin:
    serverUrl: ${KEYCLOAK_SERVER_URL:http://localhost:7080}
    defaultRealm: ${KEYCLOAK_DEFAULT_REALM:wms-realm}
```

---

## Troubleshooting

### Email Not Received

**Possible Causes:**
1. SMTP configuration incorrect
2. Email in spam folder
3. SMTP server blocking emails
4. Keycloak email sending disabled

**Solutions:**
1. Test SMTP connection in Keycloak Admin Console
2. Check spam folder
3. Verify SMTP server allows Keycloak IP
4. Check Keycloak logs for email sending errors

### Verification Link Expired

**Possible Causes:**
1. Token expiration too short
2. User waited too long before clicking link

**Solutions:**
1. Increase token expiration in Realm Settings → Tokens
2. Resend verification email via admin endpoint: `POST /api/v1/users/{userId}/resend-verification`

### Redirect URI Mismatch

**Possible Causes:**
1. Redirect URI not configured in Keycloak client
2. Frontend URL mismatch

**Solutions:**
1. Verify redirect URIs in Keycloak client settings
2. Ensure `frontend.base-url` matches actual frontend URL
3. Check that redirect URI includes wildcard `*` for query parameters

### Password Setup Not Working

**Possible Causes:**
1. Token expired
2. Token already used
3. Password doesn't meet requirements

**Solutions:**
1. Request new password reset email
2. Ensure password meets strength requirements (8+ chars, uppercase, lowercase, number, special char)
3. Check Keycloak password policies

---

## Security Best Practices

1. **Use HTTPS in Production:** All redirect URIs should use HTTPS
2. **Token Expiration:** Set appropriate token expiration (24 hours recommended)
3. **SMTP Security:** Use TLS/SSL for SMTP connections
4. **Rate Limiting:** Configure rate limiting for email sending (if available)
5. **Email Validation:** Validate email addresses before sending
6. **Logging:** Enable logging for email sending and verification attempts

---

## Testing Checklist

**Local Development (MailHog):**

- [ ] MailHog container is running
- [ ] MailHog UI accessible at `http://localhost:8025`
- [ ] Keycloak SMTP configured to use MailHog (localhost:1025)
- [ ] SMTP connection test successful in Keycloak
- [ ] Email verification email appears in MailHog inbox
- [ ] Verification link in MailHog email works and redirects correctly
- [ ] Password reset email appears in MailHog inbox
- [ ] Password setup link in MailHog email works and redirects correctly
- [ ] Expired tokens show appropriate error messages
- [ ] Invalid tokens show appropriate error messages
- [ ] Password strength validation works
- [ ] Users cannot login until email verified and password set

**Production:**

- [ ] SMTP connection test successful
- [ ] Email verification email received in real email inbox
- [ ] Verification link works and redirects correctly
- [ ] Password reset email received in real email inbox
- [ ] Password setup link works and redirects correctly
- [ ] Expired tokens show appropriate error messages
- [ ] Invalid tokens show appropriate error messages
- [ ] Password strength validation works
- [ ] Users cannot login until email verified and password set

---

## References

- [Keycloak Email Configuration Documentation](https://www.keycloak.org/docs/latest/server_admin/#_email)
- [Keycloak Action Tokens Documentation](https://www.keycloak.org/docs/latest/server_admin/#_action_tokens)
- [Keycloak Email Templates Documentation](https://www.keycloak.org/docs/latest/server_development/#_themes)

---

**Document Control**

- **Version:** 1.0
- **Last Updated:** 2025-01
- **Status:** Configuration Guide

