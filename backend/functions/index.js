const functions = require('firebase-functions');
const admin = require('firebase-admin');
const express = require('express');
const axios = require('axios');

try { admin.initializeApp(); } catch (e) {}
const db = admin.firestore();
const app = express();
app.use(express.json());
// Optional auth verification: if Authorization bearer is present, validate and attach decoded token
app.use(async (req, _res, next) => {
  try {
    const authz = req.headers && req.headers.authorization;
    if (authz && authz.startsWith('Bearer ')) {
      const token = authz.substring('Bearer '.length);
      const decoded = await admin.auth().verifyIdToken(token);
      req.user = decoded; // includes custom claims
    }
  } catch (e) {
    // ignore; endpoints may still accept uid from body in this phase
  } finally {
    next();
  }
});

const PAYSTACK_SECRET = process.env.PAYSTACK_SECRET || (functions.config().paystack && functions.config().paystack.secret);
const PAYSTACK_BASE = 'https://api.paystack.co';

// --- Daraja (M-PESA) config ---
const DARAJA_ENV = (process.env.DARAJA_ENV || (functions.config().daraja && functions.config().daraja.env) || 'sandbox').toLowerCase();
const DARAJA_CONSUMER_KEY = process.env.DARAJA_CONSUMER_KEY || (functions.config().daraja && functions.config().daraja.consumer_key);
const DARAJA_CONSUMER_SECRET = process.env.DARAJA_CONSUMER_SECRET || (functions.config().daraja && functions.config().daraja.consumer_secret);
const DARAJA_SHORTCODE = process.env.DARAJA_SHORTCODE || (functions.config().daraja && functions.config().daraja.shortcode);
const DARAJA_TXN_TYPE = (process.env.DARAJA_TRANSACTION_TYPE || (functions.config().daraja && (functions.config().daraja.transaction_type || functions.config().daraja.txn_type)) || 'CustomerPayBillOnline');
const DARAJA_PASSKEY = process.env.DARAJA_PASSKEY || (functions.config().daraja && functions.config().daraja.passkey);
const BASE_URL_PUBLIC = process.env.BASE_URL_PUBLIC || (functions.config().app && functions.config().app.base_url_public);
const DARAJA_CALLBACK_PATH = process.env.DARAJA_CALLBACK_PATH || (functions.config().daraja && functions.config().daraja.callback_path) || '/api/payments/mpesa/callback';
const MPESA_ENABLED = String(process.env.MPESA_ENABLED || (functions.config().features && functions.config().features.mpesa_enabled) || 'true').toLowerCase() === 'true';
const CALLBACK_ALLOWLIST = (process.env.DARAJA_CALLBACK_ALLOWLIST || (functions.config().daraja && functions.config().daraja.callback_allowlist) || '')
  .split(',').map(s => s.trim()).filter(Boolean);
const ADMIN_UIDS = (process.env.ADMIN_UIDS || (functions.config().app && functions.config().app.admin_uids) || '')
  .split(',').map(s => s.trim()).filter(Boolean);
const FIREBASE_WEB_API_KEY = process.env.FIREBASE_WEB_API_KEY || (functions.config().web && functions.config().web.api_key) || '';
const FIREBASE_AUTH_DOMAIN = process.env.FIREBASE_AUTH_DOMAIN || (functions.config().web && functions.config().web.auth_domain) || '';
const FIREBASE_PROJECT_ID = process.env.FIREBASE_PROJECT_ID || (functions.config().app && functions.config().app.project_id) || '';
const EXPORT_BUCKET = process.env.EXPORT_BUCKET || (functions.config().exports && functions.config().exports.bucket) || '';
const BQ_DATASET = process.env.BIGQUERY_DATASET || (functions.config().bq && functions.config().bq.dataset) || '';
const BQ_TABLE = process.env.BIGQUERY_TABLE || (functions.config().bq && functions.config().bq.table) || '';
const FUNCTIONS_REGION = (process.env.FUNCTIONS_REGION || (functions.config().app && functions.config().app.region) || 'us-central1');

function darajaBase() {
  return DARAJA_ENV === 'production' ? 'https://api.safaricom.co.ke' : 'https://sandbox.safaricom.co.ke';
}

function formatTimestampNairobi(date = new Date()) {
  // Kenya is EAT (UTC+3), no DST. Compute by adding +3h to UTC time then format as yyyyMMddHHmmss.
  const baseMs = date instanceof Date ? date.getTime() : Date.now();
  const eat = new Date(baseMs + 3 * 3600 * 1000);
  const pad = (n) => String(n).padStart(2, '0');
  const yyyy = eat.getUTCFullYear();
  const MM = pad(eat.getUTCMonth() + 1);
  const dd = pad(eat.getUTCDate());
  const HH = pad(eat.getUTCHours());
  const mm = pad(eat.getUTCMinutes());
  const ss = pad(eat.getUTCSeconds());
  return `${yyyy}${MM}${dd}${HH}${mm}${ss}`;
}

function buildPassword(shortcode, passkey, timestamp) {
  const raw = `${shortcode}${passkey}${timestamp}`;
  return Buffer.from(raw).toString('base64');
}

async function getDarajaToken() {
  const auth = Buffer.from(`${DARAJA_CONSUMER_KEY}:${DARAJA_CONSUMER_SECRET}`).toString('base64');
  const url = `${darajaBase()}/oauth/v1/generate?grant_type=client_credentials`;
  const res = await axios.get(url, { headers: { Authorization: `Basic ${auth}` } });
  return res.data && res.data.access_token ? res.data.access_token : null;
}

async function initiateStkPush({ phoneNumber, amount, accountReference, transactionDesc, uid, planId }) {
  const timestamp = formatTimestampNairobi();
  const password = buildPassword(DARAJA_SHORTCODE, DARAJA_PASSKEY, timestamp);
  const token = await getDarajaToken();
  const url = `${darajaBase()}/mpesa/stkpush/v1/processrequest`;
  const callbackUrl = `${BASE_URL_PUBLIC || ''}${DARAJA_CALLBACK_PATH}`;
  const payload = {
    BusinessShortCode: Number(DARAJA_SHORTCODE),
    Password: password,
    Timestamp: timestamp,
    TransactionType: DARAJA_TXN_TYPE,
    Amount: Number(amount),
    PartyA: Number(phoneNumber),
    PartyB: Number(DARAJA_SHORTCODE),
    PhoneNumber: Number(phoneNumber),
    CallBackURL: callbackUrl,
    AccountReference: accountReference || planId || 'NTSA-PREMIUM',
    TransactionDesc: transactionDesc || 'NTSA Premium Subscription'
  };
  const res = await axios.post(url, payload, { headers: { Authorization: `Bearer ${token}` } });
  const data = res.data || {};
  return { timestamp, password, payload, data };
}

async function queryStkStatus(checkoutRequestId) {
  const timestamp = formatTimestampNairobi();
  const password = buildPassword(DARAJA_SHORTCODE, DARAJA_PASSKEY, timestamp);
  const token = await getDarajaToken();
  const url = `${darajaBase()}/mpesa/stkpushquery/v1/query`;
  const payload = {
    BusinessShortCode: Number(DARAJA_SHORTCODE),
    Password: password,
    Timestamp: timestamp,
    CheckoutRequestID: checkoutRequestId
  };
  const res = await axios.post(url, payload, { headers: { Authorization: `Bearer ${token}` } });
  return res.data || {};
}

function isMpesaEnabled() { return !!MPESA_ENABLED; }

// Map planId -> amount (KES)
function planAmountKES(planId) {
  switch ((planId || '').toLowerCase()) {
    case 'monthly': return 300; // KES
    case 'annual': return 1500; // KES
    case 'lifetime': return 3000; // KES
    default: return 300;
  }
}

function getClientIp(req) {
  const xf = (req.headers && (req.headers['x-forwarded-for'] || req.headers['X-Forwarded-For'])) || '';
  if (xf) return String(xf).split(',')[0].trim();
  if (req.ip) return req.ip;
  const c = req.connection || {};
  return c.remoteAddress || null;
}

function isIpAllowed(ip) {
  if (!CALLBACK_ALLOWLIST.length) return true; // allow when not configured
  if (!ip) return false;
  return CALLBACK_ALLOWLIST.some(allowed => ip === allowed || ip.startsWith(allowed));
}

function maskPhone(p) {
  if (!p) return p;
  const s = String(p);
  if (s.length < 4) return '***';
  return `${'*'.repeat(Math.max(0, s.length - 4))}${s.slice(-4)}`;
}

function logEvent(evt, data = {}) {
  const safe = { ...data };
  if (safe.phone) safe.phone = maskPhone(safe.phone);
  // Log as structured JSON for Cloud Logging (jsonPayload) to enable filtering
  console.log({ event: evt, ...safe });
}

function isAdminRequest(req) {
  const u = req.user || {};
  if (u.admin === true || (u.claims && u.claims.admin === true)) return true;
  if (u.uid && ADMIN_UIDS.includes(u.uid)) return true;
  return false;
}

app.post('/paystack/initialize', async (req, res) => {
  try {
    const { uid, plan } = req.body;
    if (!uid || !plan) return res.status(400).json({ error: 'uid and plan required' });
    // map plan to amount
    const amount = plan === 'annual' ? 50000 : 15000; // kobo (KES*100)
    const callback = req.body.callbackUrl || 'ntsa://paystack/callback';
    const init = await axios.post(PAYSTACK_BASE + '/transaction/initialize', {
      email: `user-${uid}@example.local`,
      amount: amount,
      metadata: { uid, plan },
      callback_url: callback
    }, { headers: { Authorization: `Bearer ${PAYSTACK_SECRET}` }});
    const data = init.data && init.data.data ? init.data.data : {};
    res.json({ checkoutUrl: data.authorization_url, reference: data.reference });
  } catch (e) {
    res.status(500).json({ error: 'init failed', detail: e.message });
  }
});

app.get('/paystack/verify', async (req, res) => {
  try {
    const ref = req.query.reference;
    if (!ref) return res.status(400).json({ error: 'reference required' });
    const ver = await axios.get(PAYSTACK_BASE + '/transaction/verify/' + ref, {
      headers: { Authorization: `Bearer ${PAYSTACK_SECRET}` }
    });
    const data = ver.data && ver.data.data ? ver.data.data : {};
    const metadata = data.metadata || {};
    if (data.status === 'success') {
      const uid = metadata.uid;
      const plan = metadata.plan;
      const now = Date.now();
      const expiry = plan === 'annual' ? now + 365*24*3600*1000 : now + 30*24*3600*1000;
      if (uid) {
        await db.collection('subscriptions').doc(uid).set({
          status: 'active', planType: plan, startDate: now, expiryDate: expiry, updatedAt: now
        }, { merge: true });
      }
      return res.json({ success: true });
    }
    res.json({ success: false });
  } catch (e) {
    res.status(500).json({ error: 'verify failed', detail: e.message });
  }
});

app.post('/paystack/webhook', async (req, res) => {
  try {
    const event = req.body;
    // TODO: validate x-paystack-signature
    const type = event.event;
    const data = event.data || {};
    const metadata = data.metadata || {};
    const uid = metadata.uid;
    const now = Date.now();
    if (type === 'charge.success' && uid) {
      const plan = metadata.plan || 'monthly';
      const expiry = plan === 'annual' ? now + 365*24*3600*1000 : now + 30*24*3600*1000;
      await db.collection('subscriptions').doc(uid).set({ status: 'active', planType: plan, expiryDate: expiry, updatedAt: now }, { merge: true });
    }
    if (type === 'invoice.payment_failed' && uid) {
      await db.collection('subscriptions').doc(uid).set({ status: 'past_due', updatedAt: now }, { merge: true });
    }
    res.json({ received: true });
  } catch (e) {
    res.status(500).json({ error: 'webhook failed', detail: e.message });
  }
});

exports.api = functions.region(FUNCTIONS_REGION).https.onRequest(app);

// --- M-PESA / Daraja: STK Push initiation ---
app.post('/api/payments/mpesa/stk-push', async (req, res) => {
  try {
    if (!isMpesaEnabled()) {
      return res.status(503).json({ error: 'temporarily_unavailable' });
    }
    const { uid: bodyUid, phoneNumber, planId, amount: bodyAmount } = req.body || {};
    const uid = (req.user && req.user.uid) || bodyUid;
    if (!uid || !phoneNumber || !planId) {
      return res.status(400).json({ error: 'uid, phoneNumber, planId required' });
    }
    // Normalize Kenyan phone to 2547XXXXXXXX
    const normalized = normalizeKenyanMsisdn(phoneNumber);
    if (!normalized) return res.status(400).json({ error: 'Invalid phoneNumber' });

    // Determine and validate amount
    const serverAmount = planAmountKES(planId);
    let amount = serverAmount;
    if (bodyAmount != null) {
      const num = Number(bodyAmount);
      if (!Number.isFinite(num) || num <= 0) {
        return res.status(400).json({ error: 'invalid amount' });
      }
      // Enforce exact match to server-known price to prevent tampering
      if (num !== serverAmount) {
        return res.status(400).json({ error: 'amount_mismatch', expected: serverAmount });
      }
      amount = num;
    }
    // Create a payment record in Firestore (status PENDING)
    const payments = db.collection('payments');
    const paymentRef = payments.doc();
    const now = Date.now();
    const basePayment = {
      paymentId: paymentRef.id,
      userId: uid,
      planId,
      amount,
      phone: normalized,
      phoneLast4: String(normalized).slice(-4),
      status: 'PENDING',
      createdAt: now,
      updatedAt: now
    };
    await paymentRef.set(basePayment, { merge: true });

    const resp = await initiateStkPush({ phoneNumber: normalized, amount, accountReference: planId, transactionDesc: 'NTSA Premium', uid, planId });
    const data = resp.data || {};
    // Expected fields from Daraja
    const merchantRequestId = data.MerchantRequestID || null;
    const checkoutRequestId = data.CheckoutRequestID || null;
    const responseCode = data.ResponseCode || null;
    const responseDescription = data.ResponseDescription || null;
    const customerMessage = data.CustomerMessage || null;
    await paymentRef.set({
      merchantRequestId,
      checkoutRequestId,
      resultCode: responseCode,
      resultDesc: responseDescription,
      customerMessage,
      updatedAt: Date.now()
    }, { merge: true });

    res.json({
      serverPaymentId: paymentRef.id,
      merchantRequestId,
      checkoutRequestId,
      status: 'PENDING'
    });
  } catch (e) {
    console.error('stk-push error', { error: e.message });
    res.status(500).json({ error: 'stk-push failed', detail: e.message });
  }
});

// --- Manual receipt fallback ---
// Allows a user to submit a receipt code if STK callback was missed.
// This prefers matching an existing payment by receipt; otherwise records MANUAL_PENDING for review.
app.post('/api/payments/mpesa/manual-receipt', async (req, res) => {
  try {
    const { uid: bodyUid, receipt, phoneNumber, amount, planId } = req.body || {};
    const uid = (req.user && req.user.uid) || bodyUid;
    if (!uid || !receipt || !planId) {
      return res.status(400).json({ error: 'uid, receipt, planId required' });
    }
    const code = String(receipt).trim().toUpperCase().replace(/\s+/g, '');
    if (!/^([A-Z0-9]{8,12})$/.test(code)) {
      return res.status(400).json({ error: 'invalid_receipt_format' });
    }
    const normPhone = phoneNumber ? normalizeKenyanMsisdn(phoneNumber) : null;

    // Try to find existing payment with this receipt
    const payments = db.collection('payments');
    const snap = await payments.where('mpesaReceipt', '==', code).limit(1).get();
    const now = Date.now();
    if (!snap.empty) {
      const doc = snap.docs[0];
      const p = doc.data();
      const update = { updatedAt: now };
      if (p.status !== 'PAID') update.status = 'PAID';
      if (!p.userId) update.userId = uid;
      await doc.ref.set(update, { merge: true });
      if (update.status === 'PAID' && p.status !== 'PAID') {
        await grantEntitlement(update.userId || p.userId || uid, p.planId || planId, doc.id);
      }
      logEvent('manual.match', { paymentId: doc.id, receipt: code });
      return res.json({ paymentId: doc.id, status: 'PAID', matched: true });
    }

    // Create manual pending record for admin review
    const ref = payments.doc();
    const serverAmount = planAmountKES(planId);
    const amt = (amount != null && Number(amount) > 0) ? Number(amount) : serverAmount;
    await ref.set({
      paymentId: ref.id,
      source: 'manual',
      userId: uid,
      planId,
      amount: amt,
      phone: normPhone || null,
      phoneLast4: normPhone ? String(normPhone).slice(-4) : null,
      mpesaReceipt: code,
      status: 'MANUAL_PENDING',
      createdAt: now,
      updatedAt: now
    }, { merge: true });
    logEvent('manual.created', { paymentId: ref.id, receipt: code });
    return res.json({ paymentId: ref.id, status: 'MANUAL_PENDING', matched: false });
  } catch (e) {
    console.error('manual-receipt error', { error: e.message });
    return res.status(500).json({ error: 'manual failed', detail: e.message });
  }
});

// --- Daraja callback receiver ---
app.post('/api/payments/mpesa/callback', async (req, res) => {
  try {
    const ip = getClientIp(req);
    if (!isIpAllowed(ip)) {
      logEvent('callback.blocked', { ip });
      return res.status(403).json({ error: 'forbidden' });
    }
    const body = req.body || {};
    const callback = ((body.Body || {}).stkCallback) || {};
    const merchantRequestId = callback.MerchantRequestID || null;
    const checkoutRequestId = callback.CheckoutRequestID || null;
    const resultCode = callback.ResultCode;
    const resultDesc = callback.ResultDesc;
    const items = (((callback.CallbackMetadata || {}).Item) || []);
    const byName = Object.fromEntries(items.map(i => [i.Name, i.Value]));
    const mpesaReceipt = byName['MpesaReceiptNumber'] || byName['MpesaReceipt'] || null;
    const amount = byName['Amount'] || null;
    const phone = byName['PhoneNumber'] || byName['MSISDN'] || null;

    // Find payment by checkoutRequestId (primary) or merchantRequestId (fallback)
    const payments = db.collection('payments');
    let paymentDoc = null;
    if (checkoutRequestId) {
      const snap = await payments.where('checkoutRequestId', '==', checkoutRequestId).limit(1).get();
      if (!snap.empty) paymentDoc = snap.docs[0];
    }
    if (!paymentDoc && merchantRequestId) {
      const snap = await payments.where('merchantRequestId', '==', merchantRequestId).limit(1).get();
      if (!snap.empty) paymentDoc = snap.docs[0];
    }
    if (!paymentDoc) {
      logEvent('callback.orphan', { checkoutRequestId, merchantRequestId });
      // Accept for idempotency; but store an orphan log
      await db.collection('payments_orphans').add({
        type: 'daraja_callback', checkoutRequestId, merchantRequestId, resultCode, resultDesc, raw: body, createdAt: Date.now()
      });
      return res.json({ received: true });
    }

    const payment = paymentDoc.data();
    // Idempotency: if status is already terminal, do nothing beyond storing raw
    const terminal = ['PAID', 'FAILED', 'CANCELLED', 'TIMEOUT'];
    const updates = {
      resultCode,
      resultDesc,
      mpesaReceipt,
      amount: amount || payment.amount,
      phone: phone || payment.phone,
      phoneLast4: String(phone || payment.phone || '').slice(-4),
      rawCallbackJson: body,
      updatedAt: Date.now()
    };
    let newStatus = payment.status || 'PENDING';
    if (resultCode === 0 || resultCode === '0' || (resultDesc && String(resultDesc).toLowerCase().includes('success'))) {
      newStatus = 'PAID';
    } else if (resultCode === 1032) {
      newStatus = 'CANCELLED';
    } else if (resultCode === 1) {
      newStatus = 'FAILED';
    }
    if (!terminal.includes(payment.status)) updates.status = newStatus;
    await paymentDoc.ref.set(updates, { merge: true });
    logEvent('payment.updated', { paymentId: paymentDoc.id, status: updates.status, checkoutRequestId, merchantRequestId });

    // Grant entitlement on success if not already granted
    if (updates.status === 'PAID' && !terminal.includes(payment.status)) {
      await grantEntitlement(payment.userId, payment.planId, paymentDoc.id);
    }
    return res.json({ received: true });
  } catch (e) {
    console.error('callback error', { error: e.message });
    return res.status(500).json({ error: 'callback failed', detail: e.message });
  }
});

// --- Payment status ---
app.get('/api/payments/status', async (req, res) => {
  try {
    const paymentId = req.query.paymentId;
    if (!paymentId) return res.status(400).json({ error: 'paymentId required' });
    const snap = await db.collection('payments').doc(String(paymentId)).get();
    if (!snap.exists) return res.status(404).json({ error: 'not found' });
    const data = snap.data();
    // Avoid leaking PII; only include necessary fields
    res.json({
      paymentId: data.paymentId,
      status: data.status,
      planId: data.planId,
      amount: data.amount,
      checkoutRequestId: data.checkoutRequestId,
      merchantRequestId: data.merchantRequestId,
      resultCode: data.resultCode,
      resultDesc: data.resultDesc,
      mpesaReceipt: data.mpesaReceipt || null,
      updatedAt: data.updatedAt
    });
  } catch (e) {
    res.status(500).json({ error: 'status failed', detail: e.message });
  }
});

// Reconciliation: query Daraja for PENDING payments
app.post('/api/payments/mpesa/reconcile', async (req, res) => {
  try {
    if (!isAdminRequest(req)) return res.status(403).json({ error: 'forbidden' });
    const { paymentId, paymentIds, limit } = req.body || {};
    const payments = db.collection('payments');
    let targets = [];
    if (Array.isArray(paymentIds) && paymentIds.length) {
      const docs = await Promise.all(paymentIds.map(id => payments.doc(String(id)).get()));
      targets = docs.filter(d => d.exists);
    } else if (paymentId) {
      const doc = await payments.doc(String(paymentId)).get();
      if (doc.exists) targets.push(doc);
    } else {
      const snap = await payments.where('status', '==', 'PENDING').orderBy('updatedAt', 'desc').limit(Number(limit || 20)).get();
      targets = snap.docs;
    }
    const results = await reconcileDocs(targets);
    res.json({ reconciled: results.length, results });
  } catch (e) {
    res.status(500).json({ error: 'reconcile failed', detail: e.message });
  }
});

async function reconcileDocs(targets) {
  const out = [];
  for (const d of targets) {
    const p = d.data();
    if (!p.checkoutRequestId) continue;
    const q = await queryStkStatus(p.checkoutRequestId);
    const resultCode = q.ResultCode;
    const resultDesc = q.ResultDesc;
    const merchantRequestId = q.MerchantRequestID || p.merchantRequestId;
    const checkoutRequestId = q.CheckoutRequestID || p.checkoutRequestId;
    const update = { resultCode, resultDesc, merchantRequestId, checkoutRequestId, updatedAt: Date.now() };
    let newStatus = p.status || 'PENDING';
    if (resultCode === 0 || resultCode === '0') newStatus = 'PAID';
    else if (String(resultDesc || '').toLowerCase().includes('timeout')) newStatus = 'TIMEOUT';
    else if (resultCode) newStatus = 'FAILED';
    if (newStatus !== p.status) update.status = newStatus;
    await d.ref.set(update, { merge: true });
    if (update.status === 'PAID' && p.status !== 'PAID') {
      await grantEntitlement(p.userId, p.planId, d.id);
    }
    out.push({ paymentId: d.id, status: update.status || p.status, resultCode, resultDesc });
  }
  return out;
}

// Scheduled reconcile (Cloud Scheduler). Runs only when MPESA is enabled.
exports.reconcileMpesa = functions.region(FUNCTIONS_REGION).pubsub.schedule('every 15 minutes').timeZone('Africa/Nairobi').onRun(async () => {
  if (!isMpesaEnabled()) return null;
  const snap = await admin.firestore().collection('payments').where('status', '==', 'PENDING').orderBy('updatedAt', 'desc').limit(50).get();
  const results = await reconcileDocs(snap.docs);
  logEvent('reconcile.run', { count: results.length });
  return null;
});

// Scheduled daily CSV export for PAID in last 24h
exports.exportDailyPaidCsv = functions.region(FUNCTIONS_REGION).pubsub.schedule('every day 02:00').timeZone('Africa/Nairobi').onRun(async () => {
  try {
    const bucketName = EXPORT_BUCKET || `${FIREBASE_PROJECT_ID}.appspot.com`;
    const bucket = admin.storage().bucket(bucketName);
    const since = Date.now() - 24 * 3600 * 1000;
    const snap = await admin.firestore().collection('payments')
      .where('status', '==', 'PAID')
      .where('updatedAt', '>=', since)
      .orderBy('updatedAt', 'desc')
      .limit(5000)
      .get();
    const rows = snap.docs.map(d => {
      const r = d.data();
      return {
        paymentId: r.paymentId || d.id,
        userId: r.userId || '',
        status: r.status || '',
        planId: r.planId || '',
        amount: r.amount || '',
        phoneMasked: maskPhone(r.phone),
        phoneLast4: String(r.phoneLast4 || '').slice(-4),
        mpesaReceipt: r.mpesaReceipt || '',
        checkoutRequestId: r.checkoutRequestId || '',
        merchantRequestId: r.merchantRequestId || '',
        resultCode: r.resultCode || '',
        resultDesc: r.resultDesc || '',
        createdAt: r.createdAt || '',
        updatedAt: r.updatedAt || ''
      };
    });
    const csv = toCsv(rows);
    const ts = new Date();
    const pad = (n)=>String(n).padStart(2,'0');
    const yyyy = ts.getUTCFullYear();
    const MM = pad(ts.getUTCMonth()+1);
    const dd = pad(ts.getUTCDate());
    const filename = `exports/payments_${yyyy}${MM}${dd}.csv`;
    const file = bucket.file(filename);
    await file.save(csv, { contentType: 'text/csv' });
    logEvent('export.daily', { bucket: bucketName, filename, count: rows.length });
  } catch (e) {
    console.error('daily export failed', e);
  }
  return null;
});

// Scheduled daily BigQuery export for PAID in last 24h
exports.exportDailyPaidBQ = functions.region(FUNCTIONS_REGION).pubsub.schedule('every day 02:20').timeZone('Africa/Nairobi').onRun(async () => {
  if (!BQ_DATASET || !BQ_TABLE) {
    console.warn('BigQuery dataset/table not configured. Skipping export.');
    return null;
  }
  try {
    const since = Date.now() - 24 * 3600 * 1000;
    const snap = await admin.firestore().collection('payments')
      .where('status', '==', 'PAID')
      .where('updatedAt', '>=', since)
      .orderBy('updatedAt', 'desc')
      .limit(5000)
      .get();
    const rows = snap.docs.map(d => {
      const r = d.data();
      return {
        paymentId: r.paymentId || d.id,
        userId: r.userId || null,
        status: r.status || null,
        planId: r.planId || null,
        amount: r.amount || null,
        phoneLast4: String(r.phoneLast4 || '').slice(-4) || null,
        mpesaReceipt: r.mpesaReceipt || null,
        checkoutRequestId: r.checkoutRequestId || null,
        merchantRequestId: r.merchantRequestId || null,
        resultCode: r.resultCode || null,
        resultDesc: r.resultDesc || null,
        createdAt: r.createdAt ? new Date(r.createdAt) : null,
        updatedAt: r.updatedAt ? new Date(r.updatedAt) : null
      };
    });
    if (!rows.length) return null;
    const { BigQuery } = require('@google-cloud/bigquery');
    const bq = new BigQuery();
    await bq.dataset(BQ_DATASET).table(BQ_TABLE).insert(rows);
    logEvent('export.daily.bq', { dataset: BQ_DATASET, table: BQ_TABLE, count: rows.length });
  } catch (e) {
    console.error('daily bq export failed', e);
  }
  return null;
});

// --- Entitlements ---
app.get('/api/me/entitlements', async (req, res) => {
  try {
    const uid = req.query.uid;
    if (!uid) return res.status(400).json({ error: 'uid required' });
    const doc = await db.collection('subscriptions').doc(String(uid)).get();
    if (!doc.exists) return res.json({ status: 'free' });
    const sub = doc.data() || {};
    res.json({ status: sub.status || 'free', plan: sub.planType || null, expiresAt: sub.expiryDate || null });
  } catch (e) {
    res.status(500).json({ error: 'entitlements failed', detail: e.message });
  }
});

// --- Admin payments search (requires admin claim or ADMIN_UIDS allowlist) ---
app.get('/api/admin/payments', async (req, res) => {
  try {
    if (!isAdminRequest(req)) return res.status(403).json({ error: 'forbidden' });
    const { paymentId, phone, status, limit } = req.query || {};
    const payments = db.collection('payments');
    let results = [];
    if (paymentId) {
      const doc = await payments.doc(String(paymentId)).get();
      if (doc.exists) results.push({ id: doc.id, ...doc.data() });
    } else {
      let q = payments;
      if (status) q = q.where('status', '==', String(status));
      if (phone) {
        const normalized = normalizeKenyanMsisdn(String(phone));
        if (normalized) q = q.where('phone', '==', normalized);
        else return res.status(400).json({ error: 'invalid phone filter' });
      }
      const lim = Math.min(100, Math.max(1, Number(limit || 50)));
      const snap = await q.orderBy('updatedAt', 'desc').limit(lim).get();
      results = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    }
    const sanitized = results.map(r => ({
      paymentId: r.paymentId || r.id,
      status: r.status,
      planId: r.planId,
      amount: r.amount,
      phoneMasked: maskPhone(r.phone),
      mpesaReceipt: r.mpesaReceipt || null,
      checkoutRequestId: r.checkoutRequestId || null,
      merchantRequestId: r.merchantRequestId || null,
      createdAt: r.createdAt || null,
      updatedAt: r.updatedAt || null
    }));
    res.json({ count: sanitized.length, items: sanitized });
  } catch (e) {
    res.status(500).json({ error: 'admin payments failed', detail: e.message });
  }
});

// Admin: get single payment (sanitized with masked raw JSON)
app.get('/api/admin/payments/:id', async (req, res) => {
  try {
    if (!isAdminRequest(req)) return res.status(403).json({ error: 'forbidden' });
    const id = String(req.params.id);
    const doc = await db.collection('payments').doc(id).get();
    if (!doc.exists) return res.status(404).json({ error: 'not found' });
    const r = doc.data();
    const sanitized = {
      paymentId: r.paymentId || doc.id,
      userId: r.userId || '',
      status: r.status || '',
      planId: r.planId || '',
      amount: r.amount || '',
      phoneMasked: maskPhone(r.phone),
      phoneLast4: String(r.phoneLast4 || '').slice(-4),
      mpesaReceipt: r.mpesaReceipt || '',
      checkoutRequestId: r.checkoutRequestId || '',
      merchantRequestId: r.merchantRequestId || '',
      resultCode: r.resultCode || '',
      resultDesc: r.resultDesc || '',
      createdAt: r.createdAt || '',
      updatedAt: r.updatedAt || ''
    };
    const raw = r.rawCallbackJson ? maskMsisdnInString(JSON.stringify(r.rawCallbackJson)) : '';
    res.json({ payment: sanitized, rawCallbackJson: raw });
  } catch (e) {
    res.status(500).json({ error: 'admin payment failed', detail: e.message });
  }
});

function maskMsisdnInString(str) {
  if (!str) return str;
  try {
    return String(str).replace(/254(\d{5})(\d{4})/g, function(_m, _mid, last4){ return '254*****' + last4; });
  } catch { return str; }
}

// --- Admin export CSV ---
app.get('/api/admin/payments/export', async (req, res) => {
  try {
    if (!isAdminRequest(req)) return res.status(403).json({ error: 'forbidden' });
    const { status, from, to, limit } = req.query || {};
    const payments = db.collection('payments');
    let q = payments;
    if (status) q = q.where('status', '==', String(status));
    // Date range filter will be applied client-side unless Firestore composite index exists
    const lim = Math.min(1000, Math.max(1, Number(limit || 500)));
    const snap = await q.orderBy('updatedAt', 'desc').limit(lim).get();
    const fromMs = from ? Number(from) : null;
    const toMs = to ? Number(to) : null;
    const rows = [];
    for (const d of snap.docs) {
      const r = d.data();
      if (fromMs && (r.updatedAt || 0) < fromMs) continue;
      if (toMs && (r.updatedAt || 0) > toMs) continue;
      rows.push({
        paymentId: r.paymentId || d.id,
        userId: r.userId || '',
        status: r.status || '',
        planId: r.planId || '',
        amount: r.amount || '',
        phoneMasked: maskPhone(r.phone),
        phoneLast4: r.phoneLast4 || '',
        mpesaReceipt: r.mpesaReceipt || '',
        checkoutRequestId: r.checkoutRequestId || '',
        merchantRequestId: r.merchantRequestId || '',
        resultCode: r.resultCode || '',
        resultDesc: r.resultDesc || '',
        createdAt: r.createdAt || '',
        updatedAt: r.updatedAt || ''
      });
    }
    const csv = toCsv(rows);
    res.setHeader('Content-Type', 'text/csv');
    res.setHeader('Content-Disposition', 'attachment; filename="payments_export.csv"');
    res.send(csv);
  } catch (e) {
    res.status(500).json({ error: 'export failed', detail: e.message });
  }
});

function toCsv(rows) {
  if (!rows.length) return '';
  const headers = Object.keys(rows[0]);
  const esc = (v) => {
    if (v === null || v === undefined) return '';
    const s = String(v);
    if (/[",\n]/.test(s)) return '"' + s.replace(/"/g, '""') + '"';
    return s;
  };
  const lines = [headers.join(',')];
  for (const r of rows) {
    lines.push(headers.map(h => esc(r[h])).join(','));
  }
  return lines.join('\n');
}

// Helpers
function normalizeKenyanMsisdn(input) {
  if (!input) return null;
  const s = String(input).replace(/\D+/g, '');
  if (s.startsWith('2547') && s.length === 12) return s;
  if (s.startsWith('07') && s.length === 10) return '254' + s.substring(1);
  if (s.startsWith('7') && s.length === 9) return '254' + s;
  if (s.startsWith('2541') || s.startsWith('01')) { // Safaricom 01xx range
    if (s.startsWith('01') && s.length === 10) return '254' + s.substring(1);
    if (s.startsWith('2541') && s.length === 12) return s;
  }
  return null;
}

async function grantEntitlement(uid, planId, sourcePaymentId) {
  const now = Date.now();
  let durationMs = 30 * 24 * 3600 * 1000; // monthly default
  if ((planId || '').toLowerCase() === 'annual') durationMs = 365 * 24 * 3600 * 1000;
  if ((planId || '').toLowerCase() === 'lifetime') durationMs = 50 * 365 * 24 * 3600 * 1000; // effectively lifetime
  const expiresAt = now + durationMs;
  await db.collection('subscriptions').doc(String(uid)).set({
    status: 'active',
    planType: planId,
    startDate: now,
    expiryDate: expiresAt,
    updatedAt: now,
    sourcePaymentId
  }, { merge: true });
}

// Parse Daraja callback payload into normalized structure (testable)
function parseDarajaCallback(body) {
  const callback = ((body && body.Body) ? body.Body.stkCallback : null) || {};
  const items = (((callback.CallbackMetadata || {}).Item) || []);
  const byName = Object.fromEntries(items.map(i => [i.Name, i.Value]));
  return {
    merchantRequestId: callback.MerchantRequestID || null,
    checkoutRequestId: callback.CheckoutRequestID || null,
    resultCode: callback.ResultCode,
    resultDesc: callback.ResultDesc,
    amount: byName['Amount'] || null,
    mpesaReceipt: byName['MpesaReceiptNumber'] || byName['MpesaReceipt'] || null,
    phone: byName['PhoneNumber'] || byName['MSISDN'] || null
  };
}

// Expose helpers for tests
module.exports.__test__ = {
  formatTimestampNairobi,
  buildPassword,
  normalizeKenyanMsisdn,
  parseDarajaCallback
};

// --- Admin UI (served at /admin within this function) ---
app.get('/admin', (req, res) => {
  const fb = {
    apiKey: FIREBASE_WEB_API_KEY,
    authDomain: FIREBASE_AUTH_DOMAIN || `${FIREBASE_PROJECT_ID}.firebaseapp.com`,
    projectId: FIREBASE_PROJECT_ID,
  };
  const html = `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Admin - Payments</title>
  <style>
    body { font-family: system-ui, sans-serif; margin: 16px; }
    header { display:flex; justify-content: space-between; align-items:center; }
    table { border-collapse: collapse; width: 100%; margin-top: 12px; }
    th, td { border: 1px solid #ddd; padding: 8px; font-size: 14px; }
    th { background: #f2f2f2; text-align: left; }
    input, button, select { margin-right: 8px; padding: 6px; }
    .row { margin: 12px 0; }
    .muted { color: #666; }
    .ok { color: #0a0; }
    .bad { color: #a00; }
    .modal { position: fixed; top:0; left:0; right:0; bottom:0; background: rgba(0,0,0,0.6); display: none; align-items:center; justify-content: center; }
    .modal .content { background: #fff; padding: 16px; max-width: 800px; max-height: 80vh; overflow:auto; border-radius: 6px; }
    .modal.show { display: flex; }
    pre { white-space: pre-wrap; word-wrap: break-word; }
  </style>
  <script>window.__FB = ${JSON.stringify(fb)}</script>
</head>
<body>
  <header>
    <h1>Payments Admin</h1>
    <div id="authBox">
      <span id="userEmail" class="muted">Not signed in</span>
      <button id="btnSignOut" style="display:none">Sign out</button>
    </div>
  </header>
  <section>
    <div class="row">
      <input id="email" placeholder="Email" type="email" />
      <input id="password" placeholder="Password" type="password" />
      <button id="btnSignIn">Sign in</button>
      <button id="btnGoogle">Sign in with Google</button>
    </div>
    <div class="row">
      <select id="status">
        <option value="">(any status)</option>
        <option>PAID</option>
        <option>PENDING</option>
        <option>FAILED</option>
        <option>CANCELLED</option>
        <option>TIMEOUT</option>
      </select>
      <input id="phone" placeholder="Phone 07xxxxxxxx or 2547..." />
      <input id="limit" placeholder="Limit" value="50" size="4" />
      <input id="from" type="date" />
      <input id="to" type="date" />
      <button id="btnSearch">Search</button>
      <button id="btnExport">Export CSV</button>
      <button id="btnReconAll">Reconcile All Listed</button>
    </div>
    <div id="statusText" class="muted"></div>
    <table id="results"><thead><tr>
      <th>Payment ID</th><th>Status</th><th>Plan</th><th>Amount</th><th>Phone</th><th>Receipt</th><th>Checkout</th><th>Merchant</th><th>Updated</th><th>Actions</th>
    </tr></thead><tbody></tbody></table>
  </section>
  <div id="modal" class="modal"><div class="content">
    <div style="display:flex; justify-content:space-between; align-items:center; gap:8px;">
      <strong>Payment Details</strong>
      <span style="flex:1"></span>
      <button id="btnModalRecon">Reconcile</button>
      <button id="btnClose">Close</button>
    </div>
    <pre id="modalText"></pre>
  </div></div>

  <script type="module">
  import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js';
  import { getAuth, signInWithEmailAndPassword, onAuthStateChanged, signOut, GoogleAuthProvider, signInWithPopup } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js';

  const fb = window.__FB || {};
  if (!fb.apiKey) console.warn('Firebase Web API key not set. Configure web.api_key in functions config.');
  const app = initializeApp(fb);
  const auth = getAuth(app);

  const qs = (s)=>document.querySelector(s);
  const userEmail = qs('#userEmail');
  const btnSignOut = qs('#btnSignOut');
  const btnSignIn = qs('#btnSignIn');
  const btnSearch = qs('#btnSearch');
  const btnExport = qs('#btnExport');
  const btnGoogle = qs('#btnGoogle');
  const btnReconAll = qs('#btnReconAll');
  const statusEl = qs('#statusText');
  const tBody = qs('#results tbody');
  const modal = qs('#modal');
  const modalText = qs('#modalText');
  const btnClose = qs('#btnClose');
  const btnModalRecon = qs('#btnModalRecon');
  let selectedPaymentId = null;

  onAuthStateChanged(auth, (u) => {
    if (u) {
      userEmail.textContent = u.email || u.uid;
      btnSignOut.style.display = '';
    } else {
      userEmail.textContent = 'Not signed in';
      btnSignOut.style.display = 'none';
    }
  });

  btnSignIn.addEventListener('click', async () => {
    const email = qs('#email').value;
    const password = qs('#password').value;
    try {
      await signInWithEmailAndPassword(auth, email, password);
    } catch (e) {
      alert('Sign in failed: ' + e.message);
    }
  });
  btnSignOut.addEventListener('click', () => signOut(auth));
  btnGoogle.addEventListener('click', async () => {
    try {
      const provider = new GoogleAuthProvider();
      await signInWithPopup(auth, provider);
    } catch (e) {
      alert('Google sign-in failed: ' + e.message);
    }
  });

  async function authFetch(url, opts={}){
    const u = auth.currentUser;
    if (!u) throw new Error('Not signed in');
    const token = await u.getIdToken();
    opts.headers = Object.assign({}, opts.headers || {}, { Authorization: 'Bearer ' + token });
    return fetch(url, opts);
  }

  function normalizePhone(input){
    if (!input) return '';
    const s = String(input).replace(/\D+/g,'');
    if (s.startsWith('2547') && s.length === 12) return s;
    if (s.startsWith('07') && s.length === 10) return '254' + s.substring(1);
    if (s.startsWith('7') && s.length === 9) return '254' + s;
    if (s.startsWith('01') && s.length === 10) return '254' + s.substring(1);
    if (s.startsWith('2541') && s.length === 12) return s;
    return s;
  }

  function toMsFromDateInput(id){
    const v = qs('#'+id).value;
    if (!v) return null;
    const d = new Date(v + 'T00:00:00');
    return d.getTime();
  }

  async function search(){
    try {
      statusEl.textContent = 'Loading...';
      tBody.innerHTML = '';
      const status = qs('#status').value;
      const phone = normalizePhone(qs('#phone').value);
      const limit = qs('#limit').value || '50';
      const url = new URL(window.location.origin + '/api/api/admin/payments');
      if (status) url.searchParams.set('status', status);
      if (phone) url.searchParams.set('phone', phone);
      url.searchParams.set('limit', limit);
      const from = toMsFromDateInput('from');
      const to = toMsFromDateInput('to');
      if (from) url.searchParams.set('from', String(from));
      if (to) url.searchParams.set('to', String(to + 24*3600*1000 - 1));
      const resp = await authFetch(url.toString());
      if (!resp.ok) throw new Error('HTTP ' + resp.status);
      const data = await resp.json();
      statusEl.textContent = 'Found ' + (data.count || (data.items||[]).length);
      for (const r of (data.items || [])){
        const tr = document.createElement('tr');
        const td = (v)=>{ const el = document.createElement('td'); el.textContent = v==null?'':String(v); return el; };
        tr.appendChild(td(r.paymentId));
        tr.appendChild(td(r.status));
        tr.appendChild(td(r.planId));
        tr.appendChild(td(r.amount));
        tr.appendChild(td(r.phoneMasked));
        tr.appendChild(td(r.mpesaReceipt));
        tr.appendChild(td(r.checkoutRequestId));
        tr.appendChild(td(r.merchantRequestId));
        tr.appendChild(td(r.updatedAt));
        const act = document.createElement('td');
        const btn = document.createElement('button');
        btn.textContent = 'Reconcile';
        if (r.status !== 'PENDING') btn.disabled = true;
        btn.addEventListener('click', async () => {
          try {
            btn.disabled = true;
            const url = new URL(window.location.origin + '/api/api/payments/mpesa/reconcile');
            const resp = await authFetch(url.toString(), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ paymentId: r.paymentId }) });
            if (!resp.ok) throw new Error('HTTP ' + resp.status);
            const out = await resp.json();
            alert('Reconciled: ' + (out.reconciled || 0));
            search();
          } catch (e) {
            alert('Reconcile failed: ' + e.message);
          } finally { btn.disabled = false; }
        });
        const btnJson = document.createElement('button');
        btnJson.textContent = 'View JSON';
        btnJson.addEventListener('click', async () => {
          try {
            const url = new URL(window.location.origin + '/api/api/admin/payments/' + encodeURIComponent(r.paymentId));
            const resp = await authFetch(url.toString());
            if (!resp.ok) throw new Error('HTTP ' + resp.status);
            const data = await resp.json();
            modalText.textContent = JSON.stringify(data, null, 2);
            selectedPaymentId = r.paymentId;
            modal.classList.add('show');
          } catch (e) {
            alert('Load JSON failed: ' + e.message);
          }
        });
        const aLogs = document.createElement('a');
        aLogs.textContent = 'View Logs';
        aLogs.style.marginRight = '8px';
        aLogs.target = '_blank';
        aLogs.href = (function(){
          const project = (window.__FB && window.__FB.projectId) || '';
          const parts = [
            'resource.type="cloud_function"',
            'resource.labels.function_name="api"',
            'jsonPayload.paymentId="' + (r.paymentId || '') + '"'
          ];
          if (r.checkoutRequestId) parts.push('jsonPayload.checkoutRequestId="' + r.checkoutRequestId + '"');
          const query = encodeURIComponent(parts.join(' AND '));
          return 'https://console.cloud.google.com/logs/query;query=' + query + ';project=' + project;
        })();
        act.appendChild(aLogs);
        act.appendChild(btnJson);
        act.appendChild(btn);
        tr.appendChild(act);
        tBody.appendChild(tr);
      }
    } catch (e) {
      statusEl.textContent = 'Error: ' + e.message;
    }
  }

  async function exportCsv(){
    try {
      statusEl.textContent = 'Exporting...';
      const status = qs('#status').value;
      const url = new URL(window.location.origin + '/api/api/admin/payments/export');
      if (status) url.searchParams.set('status', status);
      const from = toMsFromDateInput('from');
      const to = toMsFromDateInput('to');
      if (from) url.searchParams.set('from', String(from));
      if (to) url.searchParams.set('to', String(to + 24*3600*1000 - 1));
      const resp = await authFetch(url.toString());
      if (!resp.ok) throw new Error('HTTP ' + resp.status);
      const blob = await resp.blob();
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'payments_export.csv';
      document.body.appendChild(a);
      a.click();
      a.remove();
      statusEl.textContent = 'Export generated.';
    } catch (e) {
      statusEl.textContent = 'Error: ' + e.message;
    }
  }

  btnSearch.addEventListener('click', search);
  btnExport.addEventListener('click', exportCsv);

  btnReconAll.addEventListener('click', async () => {
    try {
      statusEl.textContent = 'Reconciling all listed...';
      const ids = Array.from(tBody.querySelectorAll('tr'))
        .filter(tr => (tr.children[1]?.textContent || '') === 'PENDING')
        .map(tr => tr.children[0]?.textContent)
        .filter(Boolean);
      if (!ids.length) { statusEl.textContent = 'No PENDING items listed.'; return; }
      const url = new URL(window.location.origin + '/api/api/payments/mpesa/reconcile');
      const resp = await authFetch(url.toString(), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ paymentIds: ids }) });
      if (!resp.ok) throw new Error('HTTP ' + resp.status);
      const out = await resp.json();
      statusEl.textContent = 'Reconciled: ' + (out.reconciled || 0);
      search();
    } catch (e) {
      statusEl.textContent = 'Error: ' + e.message;
    }
  });

  btnClose.addEventListener('click', () => { modal.classList.remove('show'); });
  btnModalRecon.addEventListener('click', async () => {
    if (!selectedPaymentId) return;
    try {
      btnModalRecon.disabled = true;
      const url = new URL(window.location.origin + '/api/api/payments/mpesa/reconcile');
      const resp = await authFetch(url.toString(), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ paymentId: selectedPaymentId }) });
      if (!resp.ok) throw new Error('HTTP ' + resp.status);
      const out = await resp.json();
      alert('Reconciled: ' + (out.reconciled || 0));
      modal.classList.remove('show');
      selectedPaymentId = null;
      search();
    } catch (e) {
      alert('Reconcile failed: ' + e.message);
    } finally { btnModalRecon.disabled = false; }
  });

  </script>
</body>
</html>`;
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.send(html);
});
