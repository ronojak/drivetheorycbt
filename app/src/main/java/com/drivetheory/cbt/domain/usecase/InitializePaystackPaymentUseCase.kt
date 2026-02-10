package com.drivetheory.cbt.domain.usecase

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.repository.BillingRepository

class InitializePaystackPaymentUseCase(private val repo: BillingRepository) {
    suspend operator fun invoke(uid: String, plan: String): Result<String> {
        return when (val res = repo.initializePayment(uid, plan)) {
            is Result.Success -> {
                val url = res.data.checkoutUrl
                if (url.isNullOrBlank()) Result.Error("No checkout URL returned") else Result.Success(url)
            }
            is Result.Error -> Result.Error(res.message)
            else -> Result.Error("Payment initialization not ready")
        }
    }
}
