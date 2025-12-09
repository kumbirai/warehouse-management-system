#!/bin/bash

# Script to retrieve Keycloak client secret for wms-api client
# This script uses the Keycloak Admin REST API to get the client secret

set -e

# Default values
KEYCLOAK_SERVER_URL="${KEYCLOAK_SERVER_URL:-http://localhost:7080}"
KEYCLOAK_ADMIN_REALM="${KEYCLOAK_ADMIN_REALM:-master}"
KEYCLOAK_ADMIN_USERNAME="${KEYCLOAK_ADMIN_USERNAME:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-wms-realm}"
CLIENT_ID="${CLIENT_ID:-wms-api}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Keycloak Client Secret Retrieval"
echo "=========================================="
echo "Server URL: $KEYCLOAK_SERVER_URL"
echo "Realm: $KEYCLOAK_REALM"
echo "Client ID: $CLIENT_ID"
echo ""

# Step 1: Get admin access token
echo "Step 1: Authenticating with Keycloak Admin..."
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

# Step 2: Get realm clients
echo ""
echo "Step 2: Retrieving client information..."
CLIENT_RESPONSE=$(curl -s -X GET "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM/clients?clientId=$CLIENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json")

CLIENT_UUID=$(echo "$CLIENT_RESPONSE" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

if [ -z "$CLIENT_UUID" ]; then
    echo -e "${RED}Error: Client '$CLIENT_ID' not found in realm '$KEYCLOAK_REALM'${NC}"
    echo "Response: $CLIENT_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓ Client found (UUID: $CLIENT_UUID)${NC}"

# Step 3: Get client secret
echo ""
echo "Step 3: Retrieving client secret..."
SECRET_RESPONSE=$(curl -s -X GET "$KEYCLOAK_SERVER_URL/admin/realms/$KEYCLOAK_REALM/clients/$CLIENT_UUID/client-secret" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json")

CLIENT_SECRET=$(echo "$SECRET_RESPONSE" | grep -o '"value":"[^"]*' | cut -d'"' -f4)

if [ -z "$CLIENT_SECRET" ]; then
    echo -e "${YELLOW}Warning: Client secret not found. The client might be public or the secret might not be set.${NC}"
    echo "Response: $SECRET_RESPONSE"
    echo ""
    echo "To generate a new client secret, you can:"
    echo "1. Go to Keycloak Admin Console: $KEYCLOAK_SERVER_URL"
    echo "2. Navigate to: Realm '$KEYCLOAK_REALM' > Clients > $CLIENT_ID > Credentials"
    echo "3. Click 'Regenerate Secret' if needed"
    exit 1
fi

echo -e "${GREEN}✓ Client secret retrieved successfully${NC}"

# Output the secret
echo ""
echo "=========================================="
echo "Client Secret:"
echo "=========================================="
echo "$CLIENT_SECRET"
echo ""
echo "=========================================="
echo "Environment Variable:"
echo "=========================================="
echo "export KEYCLOAK_CLIENT_SECRET=\"$CLIENT_SECRET\""
echo ""
echo "Or add to your application.yml or .env file:"
echo "KEYCLOAK_CLIENT_SECRET=$CLIENT_SECRET"
echo ""

