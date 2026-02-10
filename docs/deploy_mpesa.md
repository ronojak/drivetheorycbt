**Deploying MPESA (Daraja) to Staging/Production**

Prereqs
- Firebase CLI authenticated and project selected
- Safaricom Daraja credentials for sandbox and production
- Public base URL for callbacks (HTTPS)

Config (via helper script)
1) From repo root:
```
cd backend/functions
./config.sh \
  <PAYSTACK_SECRET> \
  <DARAJA_CONSUMER_KEY> <DARAJA_CONSUMER_SECRET> <DARAJA_SHORTCODE> <DARAJA_PASSKEY> \
  <DARAJA_ENV:sandbox|production> \
  <BASE_URL_PUBLIC> \
  [/api/payments/mpesa/callback] [true|false] [196.201.214.200,196.201.214.206] [uid1,uid2]
```

Notes
- `features.mpesa_enabled` (MPESA_ENABLED) can be toggled later without code changes
- `daraja.callback_allowlist` enforces callback IPs in production
- `app.admin_uids` is a comma-separated list of admin UIDs; can also add `admin:true` custom claim

Deploy
```
firebase deploy --only functions
```

Verify
- Initiate STK push (sandbox): POST `api/payments/mpesa/stk-push`
- Simulate callback using backend/http-requests/mpesa.http payload
- Check status and entitlements
- Ensure scheduled function `reconcileMpesa` appears in console (runs every 15 minutes)

Admin UI
- Visit `/api/admin` on your deployed function domain (e.g., https://<region>-<project>.cloudfunctions.net/api/admin)
- Sign in with Firebase Auth (email/password)
- Or click “Sign in with Google” (enable Google provider in Firebase Console)
- Ensure the user has `admin:true` custom claim or UID included in `ADMIN_UIDS`
- Use Search and Export CSV buttons to manage payments
- Use “Reconcile” button on a payment row to force a manual Daraja status query
 - Use “Reconcile All Listed” to reconcile all PENDING items currently in the results table

Web SDK Config
- Set via `functions:config:set` using the script or manually:
  - `web.api_key=...`
 - `web.auth_domain=yourapp.firebaseapp.com`
 - Enable Google provider in Firebase Console to allow Google Sign-In

Scheduled Daily CSV Export
- A scheduled function `exportDailyPaidCsv` writes a CSV of PAID transactions in the last 24h to Cloud Storage at 02:00 EAT.
- Set an explicit bucket via `exports.bucket` (or env `EXPORT_BUCKET`), else `<project>.appspot.com` is used.
- Files are saved under `exports/payments_YYYYMMDD.csv`.

BigQuery Export (Optional)
- A scheduled function `exportDailyPaidBQ` writes PAID transactions (last 24h) to BigQuery at 02:20 EAT.
- Set dataset/table via `bq.dataset` and `bq.table` (or env `BIGQUERY_DATASET`, `BIGQUERY_TABLE`).
- Ensure the service account has BigQuery Data Editor on the dataset.

One-time Setup Script
- Use `scripts/bq_setup.sh <GCP_PROJECT> <DATASET> <TABLE>` to create dataset/table using `docs/bigquery_schema.sql` and grant permissions.
- Set functions config `bq.dataset` and `bq.table` accordingly (or use config.sh with extra args).

Android App
- Set `BACKEND_BASE_URL` and `ENABLE_MPESA=true` via Gradle properties (or CI env)
- Release with remote config for toggling UI if needed

Rollback
- Server: `features.mpesa_enabled=false` (or unset) disables STK initiation
- Client: `ENABLE_MPESA=false` hides Pay with M-PESA button
