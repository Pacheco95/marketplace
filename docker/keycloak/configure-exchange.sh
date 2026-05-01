#!/bin/bash
# Configures Keycloak token-exchange permissions so marketplace-backend
# can exchange a Google One Tap credential for a Keycloak session.
# Runs automatically via the keycloak-setup service in docker-compose.yml.
set -euo pipefail

KC_URL="${KEYCLOAK_URL:-http://keycloak:8080}"
REALM="marketplace"
KCADM="/opt/keycloak/bin/kcadm.sh"

uuid() { grep -o '[0-9a-f]\{8\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{12\}' | head -1; }

echo "[keycloak-setup] Waiting for Keycloak realm to be available..."
until $KCADM config credentials \
    --server "$KC_URL" --realm master \
    --user "${KC_BOOTSTRAP_ADMIN_USERNAME:-admin}" \
    --password "${KC_BOOTSTRAP_ADMIN_PASSWORD:-admin}" 2>/dev/null; do
  sleep 3
done

echo "[keycloak-setup] Authenticated."

echo "[keycloak-setup] Enabling token-exchange permissions on Google IDP..."
$KCADM update identity-provider/instances/google/management/permissions \
    -r "$REALM" -s enabled=true

echo "[keycloak-setup] Resolving client IDs..."
REALM_MGMT=$($KCADM get clients -r "$REALM" -q clientId=realm-management --fields id | uuid)
BACKEND=$($KCADM get clients -r "$REALM" -q clientId=marketplace-backend --fields id | uuid)
echo "[keycloak-setup]   realm-management : $REALM_MGMT"
echo "[keycloak-setup]   marketplace-backend: $BACKEND"

echo "[keycloak-setup] Creating client exchange policy..."
$KCADM create "clients/$REALM_MGMT/authz/resource-server/policy/client" \
    -r "$REALM" \
    -s name=marketplace-backend-exchange \
    -s logic=POSITIVE \
    -s decisionStrategy=UNANIMOUS \
    -s "clients=[\"$BACKEND\"]" 2>/dev/null \
    || echo "[keycloak-setup]   Policy already exists — skipping."

POLICY=$($KCADM get "clients/$REALM_MGMT/authz/resource-server/policy" \
    -r "$REALM" -q name=marketplace-backend-exchange --fields id | uuid)
echo "[keycloak-setup]   policy id: $POLICY"

echo "[keycloak-setup] Locating token-exchange scope permission..."
PERM=$($KCADM get "clients/$REALM_MGMT/authz/resource-server/permission" \
    -r "$REALM" | grep -B1 "token-exchange.permission.idp" | uuid)
echo "[keycloak-setup]   permission id: $PERM"

echo "[keycloak-setup] Granting marketplace-backend the token-exchange permission..."
$KCADM update "clients/$REALM_MGMT/authz/resource-server/permission/scope/$PERM" \
    -r "$REALM" \
    -s "policies=[\"$POLICY\"]" \
    -s decisionStrategy=UNANIMOUS

echo "[keycloak-setup] Configuring Identity Provider Redirector to auto-redirect to Google..."
# Setting defaultProvider=google on the IdP Redirector execution makes Keycloak
# skip its own login page entirely and send the user straight to Google.
# This is simpler and more reliable than disabling individual form authenticators.
IDP_REDIRECTOR_ID=$($KCADM get authentication/flows/browser/executions \
    -r "$REALM" | grep -B1 '"Identity Provider Redirector"' | uuid)
if [ -n "$IDP_REDIRECTOR_ID" ]; then
    $KCADM create "authentication/executions/$IDP_REDIRECTOR_ID/config" \
        -r "$REALM" \
        -s alias=google-auto-redirect \
        -s 'config.defaultProvider=google' 2>/dev/null \
        || echo "[keycloak-setup]   Auto-redirect config already exists — skipping."
    echo "[keycloak-setup]   Google set as default IdP — login page bypassed."
else
    echo "[keycloak-setup]   WARNING: Could not locate IdP Redirector execution — skipping."
fi

echo "[keycloak-setup] Done."
