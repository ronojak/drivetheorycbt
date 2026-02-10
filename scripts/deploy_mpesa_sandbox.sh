#!/usr/bin/env bash
set -euo pipefail

# Deploy M-PESA sandbox Functions to a given Firebase project/region.
# Usage:
#   ./scripts/deploy_mpesa_sandbox.sh <PROJECT_ID> <REGION> <PAYSTACK_SECRET> <DARAJA_CONSUMER_KEY> <DARAJA_CONSUMER_SECRET> <DARAJA_PASSKEY> [<ADMIN_UIDS>]

if [ $# -lt 6 ]; then
  echo "Usage: $0 <PROJECT_ID> <REGION> <PAYSTACK_SECRET> <DARAJA_CONSUMER_KEY> <DARAJA_CONSUMER_SECRET> <DARAJA_PASSKEY> [<ADMIN_UIDS>]" >&2
  exit 1
fi

PROJECT_ID="$1"
REGION="$2"
PAYSTACK_SECRET="$3"
DARAJA_CONSUMER_KEY="$4"
DARAJA_CONSUMER_SECRET="$5"
DARAJA_PASSKEY="$6"
ADMIN_UIDS="${7:-}"

BASE_URL_PUBLIC="https://${REGION}-${PROJECT_ID}.cloudfunctions.net/api"

echo "Using project: ${PROJECT_ID}"
firebase use "$PROJECT_ID" 1>/dev/null

echo "Setting Functions config (sandbox)…"
firebase functions:config:set \
  paystack.secret="${PAYSTACK_SECRET}" \
  daraja.consumer_key="${DARAJA_CONSUMER_KEY}" \
  daraja.consumer_secret="${DARAJA_CONSUMER_SECRET}" \
  daraja.passkey="${DARAJA_PASSKEY}" \
  daraja.env="sandbox" \
  daraja.shortcode="174379" \
  daraja.transaction_type="CustomerPayBillOnline" \
  app.base_url_public="${BASE_URL_PUBLIC}" \
  app.region="${REGION}" \
  app.project_id="${PROJECT_ID}" \
  features.mpesa_enabled="true"

if [ -n "$ADMIN_UIDS" ]; then
  firebase functions:config:set app.admin_uids="${ADMIN_UIDS}"
fi

echo "Installing backend dependencies…"
(cd backend/functions && npm install)

echo "Deploying Cloud Functions to ${REGION}…"
firebase deploy --only functions

echo "Done. Base URL: ${BASE_URL_PUBLIC}"
echo "Test STK: POST ${BASE_URL_PUBLIC}/api/payments/mpesa/stk-push"

