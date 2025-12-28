#!/bin/bash

# Script to set up the wms-service-account client in Keycloak wms-realm
# This script creates the client if it doesn't exist and configures it properly
# for service-to-service authentication using client credentials flow

set -e

# Default values
KEYCLOAK_SERVER_URL="${KEYCLOAK_SERVER_URL:-http://localhost:7080}"
KEYCLOAK_ADMIN_REALM="${KEYCLOAK_ADMIN_REALM:-master}"
KEYCLOAK_ADMIN_USERNAME="${KEYCLOAK_ADMIN_USERNAME:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-wms-realm}"
CLIENT_ID="${CLIENT_ID:-wms-service-account}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Keycloak Service Account Client Setup: $CLIENT_ID"
echo "=========================================="
echo "Server URL: $KEYCLOAK_SERVER_URL"
echo "Realm: $KEYCLOAK_REALM"
echo "Client ID: $CLIENT_ID"
echo ""

# Step 1: Get admin access token
echo -e "${BLUE}Step 1: Authenticating with Keycloak Admin...${NC}"
ADMIN_TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_SERVER_URL/realms/$KEYCLOAK_ADMIN_REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$KEYCLOAK_ADMIN_USERNAME" \
  -d "password=$KEYCLOAK_ADMIN_PASSWORD" \
  -d "grant_type=password" \
  -d "client_id=admin-cli")

ADMIN_TOKEN=$(echo "$ADMIN_TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}Error: Failed to authenticate with Keycloak Admin${NC}"
    echo "Response: $ADMIN_TOKEN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓ Admin authentication successful${NC}"

# Step 2: Check if realm exists
echo ""
echo -e "${BLUE}Step 2: Checking if realm '$KEYCLOAK_REALM' exists...${NC}"
REALM_RESPONSE=$(curl -s -X GET "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -w "%{http_code}")

HTTP_CODE="${REALM_RESPONSE: -3}"

if [ "$HTTP_CODE" != "200" ]; then
    echo -e "${YELLOW}⚠ Realm '$KEYCLOAK_REALM' does not exist. Creating realm...${NC}"
    
    REALM_CONFIG='{
      "realm": "'"$KEYCLOAK_REALM"'",
      "enabled": true,
      "displayName": "Warehouse Management System",
      "loginWithEmailAllowed": true,
      "duplicateEmailsAllowed": false,
      "resetPasswordAllowed": true,
      "editUsernameAllowed": false,
      "bruteForceProtected": true,
      "permanentLockout": false,
      "maxFailureWaitSeconds": 900,
      "minimumQuickLoginWaitSeconds": 60,
      "waitIncrementSeconds": 60,
      "quickLoginCheckMilliSeconds": 1000,
      "maxDeltaTimeSeconds": 43200,
      "failureFactor": 30
    }'
    
    CREATE_REALM_RESPONSE=$(curl -s -w "%{http_code}" -X POST "$KEYCLOAK_SERVER_URL/admin/realms" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "$REALM_CONFIG")
    
    CREATE_HTTP_CODE="${CREATE_REALM_RESPONSE: -3}"
    
    if [ "$CREATE_HTTP_CODE" = "201" ]; then
        echo -e "${GREEN}✓ Realm '$KEYCLOAK_REALM' created successfully${NC}"
    else
        echo -e "${RED}Error: Failed to create realm '$KEYCLOAK_REALM'${NC}"
        echo "Response: $CREATE_REALM_RESPONSE"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Realm '$KEYCLOAK_REALM' exists${NC}"
fi

# Step 3: Check if client exists
echo ""
echo -e "${BLUE}Step 3: Checking if client '$CLIENT_ID' exists...${NC}"
CLIENT_RESPONSE=$(curl -s -X GET "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM/clients?clientId=$CLIENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json")

CLIENT_UUID=$(echo "$CLIENT_RESPONSE" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

if [ -z "$CLIENT_UUID" ]; then
    echo -e "${YELLOW}⚠ Client '$CLIENT_ID' does not exist. Creating client...${NC}"
    
    # Create client configuration for service account
    # Service accounts use client credentials flow (no user interaction)
    CLIENT_CONFIG='{
      "clientId": "'"$CLIENT_ID"'",
      "name": "WMS Service Account",
      "description": "Service account client for service-to-service authentication in Warehouse Management System",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": "",
      "redirectUris": [],
      "webOrigins": [],
      "publicClient": false,
      "bearerOnly": false,
      "consentRequired": false,
      "standardFlowEnabled": false,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "serviceAccountsEnabled": true,
      "authorizationServicesEnabled": false,
      "fullScopeAllowed": true,
      "protocol": "openid-connect",
      "attributes": {
        "access.token.lifespan": "3600",
        "client.secret.creation.time": "0"
      }
    }'
    
    CREATE_CLIENT_RESPONSE=$(curl -s -w "%{http_code}" -X POST "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM/clients" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "$CLIENT_CONFIG")
    
    CREATE_HTTP_CODE="${CREATE_CLIENT_RESPONSE: -3}"
    
    if [ "$CREATE_HTTP_CODE" = "201" ]; then
        echo -e "${GREEN}✓ Client '$CLIENT_ID' created successfully${NC}"
        
        # Get the client UUID
        CLIENT_RESPONSE=$(curl -s -X GET "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM/clients?clientId=$CLIENT_ID" \
          -H "Authorization: Bearer $ADMIN_TOKEN" \
          -H "Content-Type: application/json")
        
        CLIENT_UUID=$(echo "$CLIENT_RESPONSE" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    else
        echo -e "${RED}Error: Failed to create client '$CLIENT_ID'${NC}"
        echo "Response: $CREATE_CLIENT_RESPONSE"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Client '$CLIENT_ID' exists (UUID: $CLIENT_UUID)${NC}"
    echo -e "${BLUE}Updating client configuration...${NC}"
    
    # Update client configuration for service account
    CLIENT_UPDATE='{
      "enabled": true,
      "publicClient": false,
      "bearerOnly": false,
      "standardFlowEnabled": false,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "serviceAccountsEnabled": true,
      "fullScopeAllowed": true,
      "redirectUris": [],
      "webOrigins": []
    }'
    
    UPDATE_RESPONSE=$(curl -s -w "%{http_code}" -X PUT "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM/clients/$CLIENT_UUID" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "$CLIENT_UPDATE")
    
    UPDATE_HTTP_CODE="${UPDATE_RESPONSE: -3}"
    
    if [ "$UPDATE_HTTP_CODE" = "204" ]; then
        echo -e "${GREEN}✓ Client configuration updated${NC}"
    else
        echo -e "${YELLOW}⚠ Warning: Client update returned HTTP $UPDATE_HTTP_CODE${NC}"
    fi
fi

# Step 4: Get or generate client secret
echo ""
echo -e "${BLUE}Step 4: Retrieving client secret...${NC}"
SECRET_RESPONSE=$(curl -s -X GET "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM/clients/$CLIENT_UUID/client-secret" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json")

CLIENT_SECRET=$(echo "$SECRET_RESPONSE" | grep -o '"value":"[^"]*' | cut -d'"' -f4)

if [ -z "$CLIENT_SECRET" ]; then
    echo -e "${YELLOW}⚠ Client secret not found. Generating new secret...${NC}"
    
    # Regenerate secret
    REGEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM/clients/$CLIENT_UUID/client-secret" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json")
    
    # Get the new secret
    SECRET_RESPONSE=$(curl -s -X GET "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM/clients/$CLIENT_UUID/client-secret" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json")
    
    CLIENT_SECRET=$(echo "$SECRET_RESPONSE" | grep -o '"value":"[^"]*' | cut -d'"' -f4)
    
    if [ -z "$CLIENT_SECRET" ]; then
        echo -e "${RED}Error: Failed to generate client secret${NC}"
        echo "Response: $SECRET_RESPONSE"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Client secret generated${NC}"
else
    echo -e "${GREEN}✓ Client secret retrieved${NC}"
fi

# Step 5: Verify service account is enabled (no mapper needed for service accounts)
echo ""
echo -e "${BLUE}Step 5: Verifying service account configuration...${NC}"
CLIENT_DETAILS=$(curl -s -X GET "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM/clients/$CLIENT_UUID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json")

SERVICE_ACCOUNT_ENABLED=$(echo "$CLIENT_DETAILS" | grep -o '"serviceAccountsEnabled":true' || echo "")

if [ -n "$SERVICE_ACCOUNT_ENABLED" ]; then
    echo -e "${GREEN}✓ Service account is enabled${NC}"
    
    # Get service account user ID
    SERVICE_ACCOUNT_USER=$(curl -s -X GET "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM/clients/$CLIENT_UUID/service-account-user" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json")
    
    SERVICE_ACCOUNT_USER_ID=$(echo "$SERVICE_ACCOUNT_USER" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    
    if [ -n "$SERVICE_ACCOUNT_USER_ID" ]; then
        echo -e "${GREEN}✓ Service account user created (ID: $SERVICE_ACCOUNT_USER_ID)${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Warning: Service account is not enabled${NC}"
fi

# Output summary
echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
echo "Service Account Client Configuration:"
echo "  - Client ID: $CLIENT_ID"
echo "  - Realm: $KEYCLOAK_REALM"
echo "  - Access Type: Confidential"
echo "  - Service Accounts: Enabled"
echo "  - Client Credentials Flow: Enabled"
echo "  - Standard Flow: Disabled (not needed for service accounts)"
echo "  - Direct Access Grants: Disabled (not needed for service accounts)"
echo ""
echo "=========================================="
echo "Client Secret:"
echo "=========================================="
echo "$CLIENT_SECRET"
echo ""
echo "=========================================="
echo "Environment Variable:"
echo "=========================================="
echo "export SERVICE_ACCOUNT_CLIENT_SECRET=\"$CLIENT_SECRET\""
echo ""
echo "Or add to your application.yml:"
echo "keycloak:"
echo "  service-account:"
echo "    token-endpoint: \${KEYCLOAK_TOKEN_ENDPOINT:http://localhost:7080/realms/wms-realm/protocol/openid-connect/token}"
echo "    client-id: \${SERVICE_ACCOUNT_CLIENT_ID:wms-service-account}"
echo "    client-secret: \${SERVICE_ACCOUNT_CLIENT_SECRET:$CLIENT_SECRET}"
echo ""
echo "This secret is used for service-to-service authentication"
echo "and should be configured in all microservices' application.yml"
echo ""
echo -e "${GREEN}✓ Setup completed successfully!${NC}"
echo ""
