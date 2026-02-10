package com.drivetheory.cbt.data.mpesa

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.data.mpesa.api.MpesaApi
import com.drivetheory.cbt.data.mpesa.api.StkPushRequest
import com.drivetheory.cbt.domain.repository.MpesaRepository
import javax.inject.Inject

class MpesaRepositoryImpl @Inject constructor(
    private val api: MpesaApi
) : MpesaRepository {
    override suspend fun initiate(uid: String, phoneNumber: String, planId: String): Result<MpesaRepository.StkInit> = try {
        val amount = planAmountKES(planId)
        val resp = api.stkPush(StkPushRequest(uid, phoneNumber, planId, amount))
        Result.Success(MpesaRepository.StkInit(resp.serverPaymentId, resp.merchantRequestId, resp.checkoutRequestId, resp.status))
    } catch (e: Exception) {
        Result.Error("STK Push failed", e)
    }

    override suspend fun status(paymentId: String): Result<MpesaRepository.PaymentStatus> = try {
        val resp = api.status(paymentId)
        Result.Success(
            MpesaRepository.PaymentStatus(
                paymentId = resp.paymentId,
                status = resp.status,
                planId = resp.planId,
                amount = resp.amount,
                checkoutRequestId = resp.checkoutRequestId,
                merchantRequestId = resp.merchantRequestId,
                resultCode = resp.resultCode,
                resultDesc = resp.resultDesc,
                mpesaReceipt = resp.mpesaReceipt,
                updatedAt = resp.updatedAt
            )
        )
    } catch (e: Exception) {
        Result.Error("Status failed", e)
    }

    override suspend fun entitlements(uid: String): Result<MpesaRepository.Entitlement> = try {
        val resp = api.entitlements(uid)
        Result.Success(MpesaRepository.Entitlement(resp.status, resp.plan, resp.expiresAt))
    } catch (e: Exception) {
        Result.Error("Entitlements failed", e)
    }

    override suspend fun submitManualReceipt(uid: String, receipt: String, planId: String, phoneNumber: String?): Result<Boolean> = try {
        val amount = planAmountKES(planId)
        val resp = api.manualReceipt(
            MpesaApi.ManualReceiptRequest(
                uid = uid,
                receipt = receipt.trim(),
                planId = planId,
                phoneNumber = phoneNumber,
                amount = amount
            )
        )
        Result.Success(resp.status == "PAID")
    } catch (e: Exception) {
        Result.Error("Receipt submission failed", e)
    }

    private fun planAmountKES(planId: String): Int = when (planId.lowercase()) {
        "monthly" -> 300
        "annual" -> 1500
        "lifetime" -> 3000
        else -> 300
    }
}
