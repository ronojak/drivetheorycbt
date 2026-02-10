#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/bq_setup.sh <GCP_PROJECT> <DATASET> <TABLE>
# Requires: gcloud auth and bq CLI

if [ $# -lt 3 ]; then
  echo "Usage: $0 <GCP_PROJECT> <DATASET> <TABLE>"
  exit 1
fi

PROJECT="$1"
DATASET="$2"
TABLE="$3"

echo "Creating dataset ${DATASET} in project ${PROJECT} (if not exists)..."
bq --project_id="${PROJECT}" --location=US mk -d --default_table_expiration 0 --description "NTSA Payments" "${DATASET}" || true

echo "Creating table ${DATASET}.${TABLE} (if not exists)..."
bq --project_id="${PROJECT}" query --use_legacy_sql=false < docs/bigquery_schema.sql || true

echo "Granting BigQuery Data Editor to Cloud Functions service account (if needed)..."
SA="$(gcloud --project="${PROJECT}" iam service-accounts list --format='value(email)' | head -n 1)"
if [ -n "${SA}" ]; then
  gcloud projects add-iam-policy-binding "${PROJECT}" \
    --member="serviceAccount:${SA}" \
    --role="roles/bigquery.dataEditor" || true
  echo "Granted roles/bigquery.dataEditor to ${SA}"
fi

echo "Done. Set functions config: bq.dataset=${DATASET} bq.table=${TABLE}"

