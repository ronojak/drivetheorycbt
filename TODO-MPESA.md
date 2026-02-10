M-PESA STK Push Integration TODOs

- Backend (Firebase Functions)
  - [x] Add Daraja config and helpers (OAuth, timestamp/password)
  - [x] Implement endpoints: `POST /api/payments/mpesa/stk-push`, `POST /api/payments/mpesa/callback`
  - [x] Implement `GET /api/payments/status`, `GET /api/me/entitlements`
  - [x] Persist payments + idempotency; grant entitlements
  - [x] Unit tests for helpers and callback parsing (Jest)
  - [x] Reconciliation endpoint to query stuck PENDING transactions
  - [x] Scheduled job (Cloud Scheduler) to trigger reconcile periodically
  - [x] Callback IP allowlist (configurable) and safe structured logs
  - [x] Admin endpoints for searching payments
  - [x] Admin UI: Google sign-in, date filters, bulk reconcile
  - [x] Scheduled daily CSV export to Cloud Storage
  - [x] Feature flag to disable MPESA endpoints gracefully

- Android
  - [x] Add MpesaApi (Retrofit) + repository
  - [x] Add ViewModel + Compose UI for MPESA
  - [x] Integrate navigation entry point (button from Subscription screen)
  - [x] Add local receipt display on success
  - [x] Add manual receipt fallback UI and API
  - [ ] Add offline grace logic (24h) if already premium

- Testing Tools
  - Backend unit tests: `cd backend/functions && npm install && npm test`
  - Manual HTTP: `backend/http-requests/mpesa.http` (REST Client) or curl
  - Admin HTTP: `backend/http-requests/admin.http`
  - Admin UI: `/api/admin` (Functions URL)
  - Android unit tests: `./gradlew testDebugUnitTest` (requires Android SDK)

- Deployment Checklist
  - [ ] Set env vars (Daraja keys, passkey, shortcode) in Functions config
  - [ ] Deploy to staging with sandbox credentials
  - [ ] Test end-to-end with a real M-PESA sandbox account
  - [ ] Switch to production creds; verify callbacks and entitlements
  - [ ] Gradual app rollout; remote flag for MPESA UI
