package com.drivetheory.cbt.data.billing

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.data.billing.api.BillingApi
import com.drivetheory.cbt.data.billing.api.InitRequest
import com.drivetheory.cbt.domain.repository.BillingRepository
import javax.inject.Inject

class BillingRepositoryImpl @Inject constructor(
    private val api: BillingApi
) : BillingRepository {
    override suspend fun initializePayment(uid: String, plan: String): Result<BillingRepository.PaymentInit> = try {
        val resp = api.initialize(InitRequest(uid, plan))
        if ((resp.checkoutUrl ?: resp.reference) != null) Result.Success(BillingRepository.PaymentInit(resp.checkoutUrl, resp.reference)) else Result.Error("Empty response")
    } catch (e: Exception) {
        Result.Error("Initialize failed", e)
    }

    override suspend fun verifyPayment(reference: String): Result<Boolean> = try {
        val resp = api.verify(reference)
        Result.Success(resp.success)
    } catch (e: Exception) {
        Result.Error("Verify failed", e)
    }
}
