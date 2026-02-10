package com.drivetheory.cbt.presentation.mpesa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MpesaPhoneUtilTest {
    @Test
    fun normalizeMsisdn_variants() {
        assertEquals("254712345678", MpesaPaymentViewModel.normalizeKenyanMsisdn("0712345678"))
        assertEquals("254712345678", MpesaPaymentViewModel.normalizeKenyanMsisdn("712345678"))
        assertEquals("254712345678", MpesaPaymentViewModel.normalizeKenyanMsisdn("254712345678"))
        assertEquals("254712345678", MpesaPaymentViewModel.normalizeKenyanMsisdn("+254 712 345 678"))
        assertNull(MpesaPaymentViewModel.normalizeKenyanMsisdn("12345"))
    }
}

