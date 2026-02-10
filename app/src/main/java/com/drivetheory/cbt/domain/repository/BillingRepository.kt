package com.drivetheory.cbt.domain.repository

import com.drivetheory.cbt.core.common.Result

interface BillingRepository {
    data class PaymentInit(val checkoutUrl: String?, val reference: String?)
    suspend fun initializePayment(uid: String, plan: String): Result<PaymentInit>
    suspend fun verifyPayment(reference: String): Result<Boolean>
}
