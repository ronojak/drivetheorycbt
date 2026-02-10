**Architecture Diagram (Text)**
- Android App → Backend (Firebase Functions/Express) → Safaricom Daraja
- Flow:
  - App calls `POST /api/payments/mpesa/stk-push` with `{ uid, phoneNumber, planId }`
  - Backend gets OAuth token, builds STK Password (Shortcode+Passkey+Timestamp), posts to Daraja
  - Backend stores payment PENDING in Firestore with CheckoutRequestID
  - Daraja calls `POST /api/payments/mpesa/callback`
  - Backend verifies payload, updates payment → PAID/FAILED, grants entitlement in `subscriptions`
  - App polls `GET /api/payments/status?paymentId=`
  - App reads entitlement via `GET /api/me/entitlements?uid=` and caches locally

**Backend API Spec**
- POST `api/payments/mpesa/stk-push`
  - Request: `{ "uid": "string", "phoneNumber": "07xxxxxxxx", "planId": "monthly|annual|lifetime" }`
  - Response: `{ "serverPaymentId": "string", "merchantRequestId": "string?", "checkoutRequestId": "string?", "status": "PENDING" }`
- POST `api/payments/mpesa/callback`
  - Request: Raw Daraja JSON
  - Response: `{ "received": true }`
- GET `api/payments/status?paymentId=...`
  - Response: `{ "paymentId": "string", "status": "PENDING|PAID|FAILED|CANCELLED|TIMEOUT", "planId": "string", "amount": number, "checkoutRequestId": "string?", "merchantRequestId": "string?", "resultCode": number?, "resultDesc": "string?", "mpesaReceipt": "string?", "updatedAt": number }`
- GET `api/me/entitlements?uid=...`
  - Response: `{ "status": "free|active|past_due", "plan": "string?", "expiresAt": number? }`

Admin + Support APIs (require admin token)
- GET `api/admin/payments?status=PENDING&limit=50`
  - Query filters: `status`, `phone` (full normalized e.g., 2547xxxxxxxx), `limit` (<=100)
  - Response: `{ count, items: [{ paymentId, status, planId, amount, phoneMasked, mpesaReceipt, checkoutRequestId, merchantRequestId, createdAt, updatedAt }] }`
- GET `api/admin/payments?paymentId=...`
  - Fetch single payment by ID
  - Requires Bearer Firebase ID token with `admin:true` custom claim, or UID in `ADMIN_UIDS` allowlist
- GET `api/admin/payments/export?status=PAID&from=1706000000000&to=1709000000000&limit=500`
  - Returns text/csv, sanitized (no full phone numbers; includes phoneMasked and phoneLast4)
 - POST `api/payments/mpesa/reconcile` `{ paymentId }`
   - Reconciles a specific payment (admin-only); queries Daraja and updates status/entitlement

Scheduled Exports
- Daily CSV export to Cloud Storage with PAID payments in last 24h
- Function: `exportDailyPaidCsv` (2:00 AM Africa/Nairobi)
- Bucket: `EXPORT_BUCKET` or default `<FIREBASE_PROJECT_ID>.appspot.com` under `exports/`
- Optional BigQuery export (daily)
- Function: `exportDailyPaidBQ` (2:20 AM Africa/Nairobi)
- Env: `BIGQUERY_DATASET`, `BIGQUERY_TABLE` (must exist with schema)

**DB Schema (SQL reference for Postgres migration)**
```sql
CREATE TABLE payments (
  payment_id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  plan_id TEXT NOT NULL,
  amount INTEGER NOT NULL,
  phone TEXT NOT NULL,
  checkout_request_id TEXT,
  merchant_request_id TEXT,
  result_code INTEGER,
  result_desc TEXT,
  mpesa_receipt TEXT,
  status TEXT NOT NULL CHECK (status IN ('PENDING','PAID','FAILED','CANCELLED','TIMEOUT')),
  raw_callback_json JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_checkout ON payments (checkout_request_id);
CREATE INDEX idx_payments_merchant ON payments (merchant_request_id);

CREATE TABLE entitlements (
  user_id TEXT PRIMARY KEY,
  plan_id TEXT NOT NULL,
  starts_at TIMESTAMPTZ NOT NULL,
  expires_at TIMESTAMPTZ,
  is_active BOOLEAN NOT NULL,
  source_payment_id TEXT REFERENCES payments(payment_id)
);
```

**Pseudocode**
- OAuth Token
```
GET /oauth/v1/generate?grant_type=client_credentials
Authorization: Basic base64(consumer_key:consumer_secret)
→ access_token
```
- STK Push
```
timestamp = formatTimestampNairobi()
password = base64(shortcode + passkey + timestamp)
POST /mpesa/stkpush/v1/processrequest with JSON:
{
  BusinessShortCode, Password, Timestamp, TransactionType:
  'CustomerPayBillOnline', Amount, PartyA, PartyB, PhoneNumber,
  CallBackURL, AccountReference, TransactionDesc
}
Store CheckoutRequestID, MerchantRequestID; status=PENDING
```
- Callback Handler
```
Parse MerchantRequestID, CheckoutRequestID, ResultCode, ResultDesc, CallbackMetadata
Find payment by CheckoutRequestID or MerchantRequestID
If not found → store orphan, return received
If found and status is terminal → merge rawCallbackJson, return received
If ResultCode==0 → status=PAID; else map to FAILED/CANCELLED/TIMEOUT
Update payment with mpesaReceipt, amount, phone
If PAID and not previously terminal → grant entitlement to user (subscriptions doc)
```

**Android Screens + Navigation**
- Screens
  - Go Premium (M-PESA): phone input, plan chips, Pay button
  - Payment Progress: inline status + polling every 2.5s up to 60s; fallback retry
  - Success: receipt + activated; auto-close
  - Error states: declined, timeout, insufficient funds
- Key Kotlin classes
  - `data.mpesa.api.MpesaApi` (Retrofit)
  - `data.mpesa.MpesaRepositoryImpl` (Repository)
  - `domain.repository.MpesaRepository`
  - `presentation.mpesa.MpesaPaymentViewModel`
  - `presentation.mpesa.MpesaPaymentActivity` (Compose)
  - `presentation.mpesa.ReceiptDetailsActivity` (Compose)
    - Buttons: Share Receipt (text), Share CSV, View Logs (opens Cloud Logging with filters)
  - Existing `SubscriptionRepository` caches entitlements

Feature Flags
- Server: `features.mpesa_enabled` (Firebase functions config) or `MPESA_ENABLED` env; when disabled, `stk-push` returns 503 temporarily_unavailable.
- Client: `BuildConfig.MPESA_ENABLED` set via Gradle property `ENABLE_MPESA` to show/hide MPESA UI.

**Deployment Checklist**
- Backend
  - Add env/config: DARAJA_ENV, DARAJA_CONSUMER_KEY, DARAJA_CONSUMER_SECRET, DARAJA_SHORTCODE, DARAJA_PASSKEY, BASE_URL_PUBLIC, DARAJA_CALLBACK_PATH
  - Deploy Functions to staging with sandbox credentials
  - Test end-to-end using `backend/http-requests/mpesa.http` and real device in sandbox
  - Protect callback: restrict by IP if feasible, log correlation IDs, ensure idempotency
  - Configure `DARAJA_CALLBACK_ALLOWLIST` (comma-separated IPs) to enforce callback source in production
  - Add reconciliation job/endpoint to query stuck PENDING (future work)
  - Cloud Scheduler: deploy scheduled `reconcileMpesa` (every 15 minutes) to query and update PENDING
  - Set `ADMIN_UIDS` (comma-separated UIDs) or add custom claim `admin:true` for support users
  - Firestore indexes: queries use `where('status','==',..).orderBy('updatedAt','desc')` → create composite index if prompted
- Android
  - Set `BACKEND_BASE_URL` Gradle property
  - Set `ENABLE_MPESA=true` when enabling MPESA UI
  - Optionally set `FIREBASE_PROJECT_ID` (Gradle property) for View Logs deep links
  - Enable MPESA feature via remote config/feature flag
  - Release to internal track → staged rollout

**Rollback Plan**
- Toggle remote feature flag off in app to hide MPESA UI
- Backend `stk-push` endpoint returns 503 when feature disabled
- No breaking changes to existing Paystack flow

**Environment Variables**
- DARAJA_ENV=sandbox|production
- DARAJA_CONSUMER_KEY, DARAJA_CONSUMER_SECRET
- DARAJA_SHORTCODE, DARAJA_PASSKEY
- DARAJA_CALLBACK_PATH (e.g., /api/payments/mpesa/callback)
- BASE_URL_PUBLIC (e.g., https://api.example.com)
- DARAJA_CALLBACK_ALLOWLIST=ip1,ip2
- ADMIN_UIDS=uid1,uid2

**Testing Plan**
- Unit tests
  - Backend: timestamp/password generation, callback parsing (Jest)
  - Android: MSISDN normalization (JUnit)
- Integration tests (staging)
  - STK Push success, declined, timeout, insufficient funds
  - Wrong phone format, partial network loss, app killed mid-payment
  - Callback idempotency (send twice, entitlement created once)
- QA checklist
  - Receipt and CheckoutRequestID are logged and retrievable
  - Entitlement expiry matches plan
  - Offline grace after activation (24h) respected
