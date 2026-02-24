#!/usr/bin/env bash
# Print deployed Functions base URL and key API endpoints for DriveTheory CBT
# Usage: [REGION=us-central1] [PROJECT=<gcp-project-id>] [BASE_URL_PUBLIC=<full-base>] ./scripts/print_endpoints.sh

set -euo pipefail

# Resolve repo root (script lives in scripts/)
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Defaults
REGION="${REGION:-us-central1}"

# Derive PROJECT from env, then .firebaserc, else fallback
PROJECT="${PROJECT:-${FIREBASE_PROJECT_ID:-}}"
if [[ -z "${PROJECT}" && -f "${REPO_ROOT}/.firebaserc" ]]; then
  # naive JSON parse: extract value of projects.default
  PROJECT=$(sed -n 's/.*"default"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${REPO_ROOT}/.firebaserc" | head -n1)
fi
if [[ -z "${PROJECT}" ]]; then
  PROJECT="drivetheorycbt"
fi

# Normalize to lowercase for URL composition (GCP project IDs are lowercase)
PROJECT_LC="$(printf "%s" "${PROJECT}" | tr '[:upper:]' '[:lower:]')"

# Compose base URL if not provided
BASE_URL_PUBLIC="${BASE_URL_PUBLIC:-https://${REGION}-${PROJECT_LC}.cloudfunctions.net/api}"

cat <<EOF
Project ID:      ${PROJECT}
Region:          ${REGION}
Base URL:        ${BASE_URL_PUBLIC}

Endpoints:
- STK Push:      ${BASE_URL_PUBLIC}/api/payments/mpesa/stk-push
- Status:        ${BASE_URL_PUBLIC}/api/payments/status?paymentId=<PAYMENT_ID>
- Callback:      ${BASE_URL_PUBLIC}/api/payments/mpesa/callback
- Reconcile:     ${BASE_URL_PUBLIC}/api/payments/mpesa/reconcile
- Manual receipt:${BASE_URL_PUBLIC}/api/payments/mpesa/manual-receipt

Examples:
curl -sS -X POST "${BASE_URL_PUBLIC}/api/payments/mpesa/stk-push" \
  -H "Content-Type: application/json" \
  -d '{"uid":"test-user-123","phoneNumber":"254708374149","planId":"monthly","amount":300}'

curl -sS "${BASE_URL_PUBLIC}/api/payments/status?paymentId=<PAYMENT_ID>"
EOF

echo
echo "Tip: Override with PROJECT, REGION, or BASE_URL_PUBLIC env vars if needed."

