package com.drivetheory.cbt.domain.repository

import com.drivetheory.cbt.core.common.Result

interface MpesaRepository {
    data class StkInit(
        val serverPaymentId: String,
        val merchantRequestId: String?,
        val checkoutRequestId: String?,
        val status: String
    )
    data class PaymentStatus(
        val paymentId: String,
        val status: String,
        val planId: String?,
        val amount: Int?,
        val checkoutRequestId: String?,
        val merchantRequestId: String?,
        val resultCode: Int?,
        val resultDesc: String?,
        val mpesaReceipt: String?,
        val updatedAt: Long?
    )
    data class Entitlement(
        val status: String,
        val plan: String?,
        val expiresAt: Long?
    )

    suspend fun initiate(uid: String, phoneNumber: String, planId: String): Result<StkInit>
    suspend fun status(paymentId: String): Result<PaymentStatus>
    suspend fun entitlements(uid: String): Result<Entitlement>

    suspend fun submitManualReceipt(
        uid: String,
        receipt: String,
        planId: String,
        phoneNumber: String?
    ): Result<Boolean>
}
