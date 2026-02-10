# Backend for NTSA Practice Exams (DriveTheory CBT)

This is a minimal Node/Express (Cloud Functions compatible) backend to handle payments with server-held secrets.

Supported Providers
- Paystack (existing)
- M-PESA (Safaricom Daraja) STK Push (new)

Endpoints (Paystack)
- POST /paystack/initialize: body { uid, plan } → returns { checkoutUrl, reference }
- GET /paystack/verify?reference=... → returns { success }
- POST /paystack/webhook: Paystack event webhook; validates signature and updates Firestore subscription

Endpoints (M-PESA / Daraja)
- POST /api/payments/mpesa/stk-push: body { uid, phoneNumber, planId } → returns { checkoutRequestId, merchantRequestId, serverPaymentId, status }
- POST /api/payments/mpesa/callback: Daraja callback target; updates Firestore payment + subscription
- GET /api/payments/status?paymentId=... → returns { status, ... }
- GET /api/me/entitlements?uid=... → returns { status, plan, expiresAt }

Environment (.env)
- PAYSTACK_SECRET=sk_live_xxx (never put in the Android app)
- FIREBASE_PROJECT_ID=...
- GOOGLE_APPLICATION_CREDENTIALS=serviceAccountKey.json (if running outside Functions)
- DARAJA_ENV=sandbox|production
- DARAJA_CONSUMER_KEY=...
- DARAJA_CONSUMER_SECRET=...
- DARAJA_SHORTCODE=...
- DARAJA_PASSKEY=...
- BASE_URL_PUBLIC=https://api.example.com
- DARAJA_CALLBACK_PATH=/api/payments/mpesa/callback

Deploy
- Cloud Functions: export the Express app via functions.https.onRequest(app)
- Or run as a standalone Express server (set BASE_URL accordingly in the app).

Notes
- Secrets must not be bundled into the APK. Keep credentials in Functions config or env vars.
- For Daraja, use server time in Africa/Nairobi timezone for STK Push `Timestamp`.
