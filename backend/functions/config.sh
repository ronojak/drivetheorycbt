#!/usr/bin/env bash
# Configure Firebase Functions secrets for payment providers
# Usage:
#   ./config.sh <paystack_secret> \
#     <daraja_consumer_key> <daraja_consumer_secret> <daraja_shortcode> <daraja_passkey> <daraja_env:sandbox|production> <base_url_public> [<daraja_callback_path>] [<mpesa_enabled:true|false>] [<callback_allowlist_csv>] [<admin_uids_csv>] [<web_api_key>] [<auth_domain>] [<export_bucket>] [<bq_dataset>] [<bq_table>] [<daraja_transaction_type>] [<functions_region>]

if [ -z "$1" ]; then
  echo "Usage: $0 <paystack_secret> <daraja_consumer_key> <daraja_consumer_secret> <daraja_shortcode> <daraja_passkey> <daraja_env> <base_url_public> [<daraja_callback_path>]"
  exit 1
fi

PAYSTACK_SECRET="$1"
DARAJA_CONSUMER_KEY="$2"
DARAJA_CONSUMER_SECRET="$3"
DARAJA_SHORTCODE="$4"
DARAJA_PASSKEY="$5"
DARAJA_ENV="$6"
BASE_URL_PUBLIC="$7"
DARAJA_CALLBACK_PATH="${8:-/api/payments/mpesa/callback}"
MPESA_ENABLED="${9:-true}"
CALLBACK_ALLOWLIST="${10:-}"
ADMIN_UIDS="${11:-}"
WEB_API_KEY="${12:-}"
AUTH_DOMAIN="${13:-}"
EXPORT_BUCKET="${14:-}"
BIGQUERY_DATASET="${15:-}"
BIGQUERY_TABLE="${16:-}"
DARAJA_TRANSACTION_TYPE="${17:-}"
FUNCTIONS_REGION="${18:-}"

firebase functions:config:set \
  paystack.secret="${PAYSTACK_SECRET}" \
  daraja.consumer_key="${DARAJA_CONSUMER_KEY}" \
  daraja.consumer_secret="${DARAJA_CONSUMER_SECRET}" \
  daraja.shortcode="${DARAJA_SHORTCODE}" \
  daraja.passkey="${DARAJA_PASSKEY}" \
  daraja.env="${DARAJA_ENV}" \
  daraja.callback_path="${DARAJA_CALLBACK_PATH}" \
  app.base_url_public="${BASE_URL_PUBLIC}" \
  features.mpesa_enabled="${MPESA_ENABLED}" \
  daraja.callback_allowlist="${CALLBACK_ALLOWLIST}" \
  app.admin_uids="${ADMIN_UIDS}"
if [ -n "$DARAJA_TRANSACTION_TYPE" ]; then
  firebase functions:config:set daraja.transaction_type="${DARAJA_TRANSACTION_TYPE}"
fi
if [ -n "$FUNCTIONS_REGION" ]; then
  firebase functions:config:set app.region="${FUNCTIONS_REGION}"
fi
if [ -n "$WEB_API_KEY" ]; then
  firebase functions:config:set web.api_key="${WEB_API_KEY}"
fi
if [ -n "$AUTH_DOMAIN" ]; then
  firebase functions:config:set web.auth_domain="${AUTH_DOMAIN}"
fi
if [ -n "$EXPORT_BUCKET" ]; then
  firebase functions:config:set exports.bucket="${EXPORT_BUCKET}"
fi
if [ -n "$BIGQUERY_DATASET" ]; then
  firebase functions:config:set bq.dataset="${BIGQUERY_DATASET}"
fi
if [ -n "$BIGQUERY_TABLE" ]; then
  firebase functions:config:set bq.table="${BIGQUERY_TABLE}"
fi

echo "Config set. Deploy with: firebase deploy --only functions"
