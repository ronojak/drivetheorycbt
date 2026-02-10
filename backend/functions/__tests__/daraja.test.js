const { __test__ } = require('../index');

describe('Daraja helpers', () => {
  test('formatTimestampNairobi returns 14-digit string', () => {
    const ts = __test__.formatTimestampNairobi(new Date('2024-01-01T00:00:00Z'));
    expect(ts).toMatch(/^\d{14}$/);
    // 2024-01-01 03:00:00 in EAT
    expect(ts).toBe('20240101030000');
  });

  test('buildPassword base64 encodes shortcode+passkey+timestamp', () => {
    const s = '123456';
    const p = 'PASSKEY';
    const t = '20240101030000';
    const expected = Buffer.from(`${s}${p}${t}`).toString('base64');
    expect(__test__.buildPassword(s, p, t)).toBe(expected);
  });

  test('normalizeKenyanMsisdn formats variants to 2547XXXXXXXX', () => {
    expect(__test__.normalizeKenyanMsisdn('0712345678')).toBe('254712345678');
    expect(__test__.normalizeKenyanMsisdn('712345678')).toBe('254712345678');
    expect(__test__.normalizeKenyanMsisdn('254712345678')).toBe('254712345678');
    expect(__test__.normalizeKenyanMsisdn('+254712345678')).toBe('254712345678');
  });

  test('parseDarajaCallback extracts key fields', () => {
    const body = {
      Body: {
        stkCallback: {
          MerchantRequestID: '29115-34620561-1',
          CheckoutRequestID: 'ws_CO_0101202400000000',
          ResultCode: 0,
          ResultDesc: 'Success',
          CallbackMetadata: {
            Item: [
              { Name: 'Amount', Value: 300 },
              { Name: 'MpesaReceiptNumber', Value: 'NLJ7RT61SV' },
              { Name: 'PhoneNumber', Value: 254712345678 }
            ]
          }
        }
      }
    };
    const out = __test__.parseDarajaCallback(body);
    expect(out.merchantRequestId).toBe('29115-34620561-1');
    expect(out.checkoutRequestId).toBe('ws_CO_0101202400000000');
    expect(out.resultCode).toBe(0);
    expect(out.amount).toBe(300);
    expect(out.mpesaReceipt).toBe('NLJ7RT61SV');
    expect(out.phone).toBe(254712345678);
  });
});

